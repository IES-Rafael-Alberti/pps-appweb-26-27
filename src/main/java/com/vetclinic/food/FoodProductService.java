package com.vetclinic.food;

import com.vetclinic.common.BadRequestException;
import com.vetclinic.common.ConflictException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.food.dto.FoodProductRequest;
import com.vetclinic.food.dto.FoodProductResponse;
import com.vetclinic.sale.FoodSaleRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FoodProductService {

    private final FoodProductRepository foodProductRepository;
    private final FoodSaleRepository foodSaleRepository;

    public FoodProductService(FoodProductRepository foodProductRepository, FoodSaleRepository foodSaleRepository) {
        this.foodProductRepository = foodProductRepository;
        this.foodSaleRepository = foodSaleRepository;
    }

    @Transactional(readOnly = true)
    public FoodProductResponse findById(Long id) {
        return FoodProductResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<FoodProductResponse> search(FoodCategory category, String brand, boolean includeInactive,
                                            Integer maxStock, Pageable pageable) {
        if (maxStock != null && maxStock < 0) {
            throw new BadRequestException("maxStock cannot be negative.");
        }
        return foodProductRepository.search(category, trim(brand), includeInactive, maxStock, pageable)
                .map(FoodProductResponse::from);
    }

    @Transactional(readOnly = true)
    public List<FoodProductResponse> findLowStock(int threshold) {
        if (threshold < 0) {
            throw new BadRequestException("threshold cannot be negative.");
        }
        return foodProductRepository.findLowStock(threshold).stream()
                .map(FoodProductResponse::from)
                .toList();
    }

    public FoodProductResponse create(FoodProductRequest request) {
        String name = request.name().trim();
        String brand = request.brand().trim();
        if (foodProductRepository.existsByNameIgnoreCaseAndBrandIgnoreCase(name, brand)) {
            throw new ConflictException("Product '%s' from '%s' is already registered".formatted(name, brand));
        }
        FoodProduct product = new FoodProduct(name, brand, request.category(), trim(request.description()),
                request.unitPrice(), request.stock() == null ? 0 : request.stock(), request.weightKg());
        return FoodProductResponse.from(foodProductRepository.save(product));
    }

    public FoodProductResponse update(Long id, FoodProductRequest request) {
        FoodProduct product = getEntity(id);
        String name = request.name().trim();
        String brand = request.brand().trim();
        if (foodProductRepository.existsByNameIgnoreCaseAndBrandIgnoreCaseAndIdNot(name, brand, id)) {
            throw new ConflictException("Another product already uses the name '%s' from '%s'"
                    .formatted(name, brand));
        }
        product.updateDetails(name, brand, request.category(), trim(request.description()),
                request.unitPrice(), request.weightKg());
        return FoodProductResponse.from(product);
    }

    public FoodProductResponse adjustStock(Long id, int delta) {
        if (delta == 0) {
            throw new BadRequestException("delta cannot be 0. Use the sale endpoint to sell units.");
        }
        FoodProduct product = getEntity(id);
        try {
            product.applyStockDelta(delta);
        } catch (IllegalStateException ex) {
            throw new ConflictException("Product '%s' only has %d unit(s) in stock, cannot remove %d"
                    .formatted(product.getName(), product.getStock(), Math.abs(delta)));
        }
        return FoodProductResponse.from(product);
    }

    public FoodProductResponse setActive(Long id, boolean active) {
        FoodProduct product = getEntity(id);
        product.setActive(active);
        return FoodProductResponse.from(product);
    }

    /**
     * Products already referenced by a sale are never removed from the database, otherwise the historical
     * invoice would lose its product reference. They are deactivated instead.
     */
    public void delete(Long id) {
        FoodProduct product = getEntity(id);
        if (foodSaleRepository.existsByLinesProductId(id)) {
            product.setActive(false);
            return;
        }
        foodProductRepository.delete(product);
    }

    private FoodProduct getEntity(Long id) {
        return foodProductRepository.findById(id).orElseThrow(() -> NotFoundException.of("Food product", id));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
