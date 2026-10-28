package com.vetclinic.sale.dto;

import com.vetclinic.sale.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

@Schema(name = "CreateSaleRequest", description = "A shop ticket. The server computes unit prices and totals.")
public record CreateSaleRequest(

        @Schema(description = "Registered client. Optional, walk-in customers only need customerName.",
                example = "1")
        @Positive(message = "customerId must be a positive number")
        Long customerId,

        @Schema(example = "Marta Ruiz")
        @NotBlank(message = "customerName is required")
        @Size(max = 120, message = "customerName must be at most 120 characters")
        String customerName,

        @Schema(description = "cash, card, transfer", example = "card")
        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,

        @Schema(description = "Defaults to the current time. Use it to backdate a till entry.")
        @PastOrPresent(message = "soldAt cannot be in the future")
        Instant soldAt,

        @NotEmpty(message = "a sale must contain at least one line")
        @Valid
        List<SaleLineRequest> lines,

        @Schema(example = "Customer asked for a loyalty discount.")
        @Size(max = 500, message = "notes must be at most 500 characters")
        String notes) {

    @Schema(name = "SaleLineRequest", description = "One product and how many units to sell")
    public record SaleLineRequest(

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
