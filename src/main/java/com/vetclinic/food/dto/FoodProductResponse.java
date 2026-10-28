package com.vetclinic.food.dto;

import com.vetclinic.food.FoodCategory;
import com.vetclinic.food.FoodProduct;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "FoodProductResponse")
public record FoodProductResponse(
        Long id,
        String name,
        String brand,
        FoodCategory category,
        String description,
        BigDecimal unitPrice,
        int stock,
        BigDecimal weightKg,
        boolean active,
        long version,
        Instant createdAt) {

    public static FoodProductResponse from(FoodProduct product) {
        return new FoodProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getDescription(),
                product.getUnitPrice(),
                product.getStock(),
                product.getWeightKg(),
                product.isActive(),
                product.getVersion(),
                product.getCreatedAt());
    }
}
