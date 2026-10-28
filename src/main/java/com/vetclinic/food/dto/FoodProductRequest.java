package com.vetclinic.food.dto;

import com.vetclinic.food.FoodCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(name = "FoodProductRequest", description = "Payload to create or update a food product")
public record FoodProductRequest(

        @Schema(example = "Crocodile Adult Mini")
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Schema(example = "Royal Canin")
        @NotBlank(message = "brand is required")
        @Size(max = 80, message = "brand must be at most 80 characters")
        String brand,

        @Schema(description = "dog_food, cat_food, puppy_food, kitten_food, bird_food, rabbit_food, "
                + "treats, supplements, medicine", example = "dog_food")
        @NotNull(message = "category is required")
        FoodCategory category,

        @Schema(example = "Breed formula for small adult dogs, chicken flavour, 3 kg bag.")
        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @Schema(description = "Price per unit, in EUR. Two decimals.", example = "24.50")
        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.01", message = "unitPrice must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "unitPrice must have at most 2 decimal places")
        BigDecimal unitPrice,

        @Schema(description = "Units in stock. Only used on creation; use PATCH /api/v1/foods/{id}/stock later.",
                example = "40", defaultValue = "0")
        @Min(value = 0, message = "stock cannot be negative")
        Integer stock,

        @Schema(description = "Format weight, in kg", example = "3.000")
        @DecimalMin(value = "0.0", inclusive = false, message = "weightKg must be greater than 0")
        @Digits(integer = 4, fraction = 3, message = "weightKg must have at most 3 decimal places")
        BigDecimal weightKg) {
}
