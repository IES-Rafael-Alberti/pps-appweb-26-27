package com.vetclinic.food.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "StockAdjustmentRequest", description = "Relative stock movement. Negative values withdraw units.")
public record StockAdjustmentRequest(

        @Schema(description = "Units to add (positive) or remove (negative)", example = "12")
        @NotNull(message = "delta is required")
        Integer delta) {
}
