package com.vetclinic.food;

import com.vetclinic.common.PageResponse;
import com.vetclinic.food.dto.FoodProductRequest;
import com.vetclinic.food.dto.FoodProductResponse;
import com.vetclinic.food.dto.StockAdjustmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/foods")
@Tag(name = "Food products", description = "Catalogue and stock of the food sold in the clinic shop")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class FoodProductController {

    private final FoodProductService foodProductService;

    public FoodProductController(FoodProductService foodProductService) {
        this.foodProductService = foodProductService;
    }

    @GetMapping
    @Operation(summary = "Browse the catalogue",
            description = "Only active products are returned unless `includeInactive=true`.")
    public PageResponse<FoodProductResponse> list(
            @Parameter(description = "dog_food, cat_food, puppy_food, ...")
            @RequestParam(required = false) FoodCategory category,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @Parameter(description = "Only products with stock at or below this value")
            @RequestParam(required = false) Integer maxStock,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(
                foodProductService.search(category, brand, includeInactive, maxStock, pageable));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Products running out", description = "Active products with stock <= threshold")
    public List<FoodProductResponse> lowStock(
            @Parameter(description = "Stock level that counts as low", example = "5")
            @RequestParam(defaultValue = "5") int threshold) {
        return foodProductService.findLowStock(threshold);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public FoodProductResponse get(@PathVariable Long id) {
        return foodProductService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new product")
    public FoodProductResponse create(@Valid @RequestBody FoodProductRequest request) {
        return foodProductService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product", description = "Does not change the stock; use the stock endpoint")
    public FoodProductResponse update(@PathVariable Long id, @Valid @RequestBody FoodProductRequest request) {
        return foodProductService.update(id, request);
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Adjust stock", description = "delta > 0 receives units, delta < 0 withdraws them")
    public FoodProductResponse adjustStock(@PathVariable Long id,
                                           @Valid @RequestBody StockAdjustmentRequest request) {
        return foodProductService.adjustStock(id, request.delta());
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Withdraw a product from the catalogue without deleting it")
    public FoodProductResponse deactivate(@PathVariable Long id) {
        return foodProductService.setActive(id, false);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Put a deactivated product back on sale")
    public FoodProductResponse activate(@PathVariable Long id) {
        return foodProductService.setActive(id, true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product",
            description = "Deactivates it instead when it already appears in a past sale")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        foodProductService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
