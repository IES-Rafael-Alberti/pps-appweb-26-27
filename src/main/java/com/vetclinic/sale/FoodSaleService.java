package com.vetclinic.sale;

import com.vetclinic.common.BadRequestException;
import com.vetclinic.common.ConflictException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.food.FoodProduct;
import com.vetclinic.food.FoodProductRepository;
import com.vetclinic.owner.Owner;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.sale.dto.CreateSaleRequest;
import com.vetclinic.sale.dto.SaleResponse;
import com.vetclinic.sale.dto.TopProduct;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FoodSaleService {

    private final FoodSaleRepository foodSaleRepository;
    private final FoodProductRepository foodProductRepository;
    private final OwnerRepository ownerRepository;

    public FoodSaleService(FoodSaleRepository foodSaleRepository,
                           FoodProductRepository foodProductRepository,
                           OwnerRepository ownerRepository) {
        this.foodSaleRepository = foodSaleRepository;
        this.foodProductRepository = foodProductRepository;
        this.ownerRepository = ownerRepository;
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        FoodSale sale = foodSaleRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Sale", id));
        return SaleResponse.from(sale);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> search(Long customerId, PaymentMethod paymentMethod, Instant from, Instant to,
                                      Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'.");
        }
        return foodSaleRepository.search(customerId, paymentMethod, from, to, pageable)
                .map(SaleResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TopProduct> topProducts(int limit) {
        if (limit < 1) {
            throw new BadRequestException("limit must be at least 1.");
        }
        return foodSaleRepository.topProductsByRevenue(Pageable.ofSize(limit));
    }

    /**
     * Registers a shop ticket and discounts the stock of every line in a single transaction.
     *
     * <p>The involved products are locked with {@code SELECT ... FOR UPDATE} in ascending id order, so two
     * cash registers selling the last bag at the same time cannot oversell: the second transaction waits and
     * then fails with 409 because the stock has already been consumed.
     */
    public SaleResponse registerSale(CreateSaleRequest request) {
        List<CreateSaleRequest.SaleLineRequest> lines = request.lines();
        Set<Long> productIds = new LinkedHashSet<>();
        for (CreateSaleRequest.SaleLineRequest line : lines) {
            if (!productIds.add(line.productId())) {
                throw new BadRequestException(
                        "Product %d appears in more than one line. Merge the quantities into a single line."
                                .formatted(line.productId()));
            }
        }

        List<Long> orderedIds = productIds.stream().sorted().toList();
        Map<Long, FoodProduct> productsById = foodProductRepository.findAllByIdForUpdate(orderedIds).stream()
                .collect(Collectors.toMap(FoodProduct::getId, Function.identity()));
        for (Long productId : orderedIds) {
            if (!productsById.containsKey(productId)) {
                throw NotFoundException.of("Food product", productId);
            }
        }

        Owner customer = null;
        if (request.customerId() != null) {
            customer = ownerRepository.findById(request.customerId())
                    .orElseThrow(() -> NotFoundException.of("Owner", request.customerId()));
        }

        Instant soldAt = request.soldAt() == null ? Instant.now() : request.soldAt();
        FoodSale sale = new FoodSale(soldAt, request.customerName().trim(), customer,
                request.paymentMethod(), trim(request.notes()));

        for (CreateSaleRequest.SaleLineRequest line : lines) {
            FoodProduct product = productsById.get(line.productId());
            if (!product.isActive()) {
                throw new ConflictException("Product '%s' is withdrawn from the catalogue and cannot be sold"
                        .formatted(product.getName()));
            }
            if (product.getStock() < line.quantity()) {
                throw new ConflictException("Not enough stock for '%s': requested %d, available %d"
                        .formatted(product.getName(), line.quantity(), product.getStock()));
            }
            sale.addLine(product, line.quantity());
        }

        // flush to obtain the generated id, which is part of the human readable ticket number
        foodSaleRepository.saveAndFlush(sale);
        sale.assignSaleNumber();
        return SaleResponse.from(sale);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
