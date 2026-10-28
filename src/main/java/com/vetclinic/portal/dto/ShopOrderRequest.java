package com.vetclinic.portal.dto;

import com.vetclinic.sale.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(name = "ShopOrderRequest",
        description = "Online order. Prices and totals are computed by the server from the catalogue.")
public record ShopOrderRequest(

        @NotEmpty(message = "an order must contain at least one line")
        @Valid
        List<Line> lines,

        @Schema(description = "cash, card, transfer", example = "card")
        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,

        @Schema(example = "Recojo el sábado por la mañana.")
        @Size(max = 500, message = "notes must be at most 500 characters")
        String notes) {

    @Schema(name = "ShopOrderLine")
    public record Line(

            @Schema(example = "1")
            @NotNull(message = "lines[].productId is required")
            @Positive(message = "lines[].productId must be a positive number")
            Long productId,

            @Schema(example = "2")
            @NotNull(message = "lines[].quantity is required")
            @Min(value = 1, message = "lines[].quantity must be at least 1")
            Integer quantity) {
    }
}
