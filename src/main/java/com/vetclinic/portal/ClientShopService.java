package com.vetclinic.portal;

import com.vetclinic.common.ForbiddenException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.food.FoodCategory;
import com.vetclinic.food.FoodProductService;
import com.vetclinic.food.dto.FoodProductResponse;
import com.vetclinic.owner.Owner;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.portal.dto.ShopOrderRequest;
import com.vetclinic.sale.FoodSaleService;
import com.vetclinic.sale.dto.CreateSaleRequest;
import com.vetclinic.sale.dto.SaleResponse;
import com.vetclinic.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client facing shop. Reuses the staff catalogue and the transactional sale service, so stock is
 * discounted atomically and the client can never choose a price: only product ids and quantities.
 */
@Service
@Transactional
public class ClientShopService {

    private final FoodProductService foodProductService;
    private final FoodSaleService foodSaleService;
    private final OwnerRepository ownerRepository;

    public ClientShopService(FoodProductService foodProductService,
                             FoodSaleService foodSaleService,
                             OwnerRepository ownerRepository) {
        this.foodProductService = foodProductService;
        this.foodSaleService = foodSaleService;
        this.ownerRepository = ownerRepository;
    }

    @Transactional(readOnly = true)
    public Page<FoodProductResponse> catalogue(FoodCategory category, String brand, Pageable pageable) {
        return foodProductService.search(category, brand, false, null, pageable);
    }

    @Transactional(readOnly = true)
    public FoodProductResponse product(Long id) {
        FoodProductResponse product = foodProductService.findById(id);
        if (!product.active()) {
            throw NotFoundException.of("Food product", id);
        }
        return product;
    }

    public SaleResponse order(CurrentUser currentUser, ShopOrderRequest request) {
        Owner owner = owner(currentUser);
        var lines = request.lines().stream()
                .map(line -> new CreateSaleRequest.SaleLineRequest(line.productId(), line.quantity()))
                .toList();
        return foodSaleService.registerSale(new CreateSaleRequest(
                owner.getId(), owner.getFullName(), request.paymentMethod(), null, lines,
                request.notes() == null || request.notes().isBlank() ? null : request.notes().trim()));
    }

    private Owner owner(CurrentUser currentUser) {
        if (currentUser.ownerId() == null) {
            throw new ForbiddenException("This account is not linked to a clinic client profile.");
        }
        return ownerRepository.findById(currentUser.ownerId())
                .orElseThrow(() -> new ForbiddenException("The clinic profile of this account no longer exists."));
    }
}
