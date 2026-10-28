package com.vetclinic.portal;

import com.vetclinic.common.PageResponse;
import com.vetclinic.food.FoodCategory;
import com.vetclinic.food.dto.FoodProductResponse;
import com.vetclinic.portal.dto.ShopOrderRequest;
import com.vetclinic.sale.dto.SaleResponse;
import com.vetclinic.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The food shop as seen by a client: browse the active catalogue and place an online order. */
@RestController
@RequestMapping("/api/v1/me/shop")
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client shop", description = "Browse the food catalogue and place an order as a client")
public class ClientShopController {

    private final ClientShopService clientShopService;

    public ClientShopController(ClientShopService clientShopService) {
        this.clientShopService = clientShopService;
    }

    @GetMapping("/products")
    @Operation(summary = "Browse the shop catalogue",
            description = "Only active products are returned. Withdrawn products are invisible to clients.")
    public PageResponse<FoodProductResponse> products(
            @Parameter(description = "dog_food, cat_food, puppy_food, ...")
            @RequestParam(required = false) FoodCategory category,
            @RequestParam(required = false) String brand,
            @PageableDefault(size = 24, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(clientShopService.catalogue(category, brand, pageable));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get a catalogue product by id")
    public FoodProductResponse product(@PathVariable Long id) {
        return clientShopService.product(id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order",
            description = "Creates a sale for the logged in client, computes prices and totals on the "
                    + "server and discounts stock in the same transaction. Out of stock answers 409.")
    public SaleResponse order(@AuthenticationPrincipal CurrentUser currentUser,
                              @Valid @RequestBody ShopOrderRequest request) {
        return clientShopService.order(currentUser, request);
    }
}
