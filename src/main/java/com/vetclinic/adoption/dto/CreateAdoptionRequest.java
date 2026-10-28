package com.vetclinic.adoption.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "CreateAdoptionRequest", description = "Payload to adopt a shelter animal")
public record CreateAdoptionRequest(

        @Schema(description = "Pet that must currently be 'available'", example = "1")
        @NotNull(message = "petId is required")
        @Positive(message = "petId must be a positive number")
        Long petId,

        @Schema(description = "Client that adopts the animal", example = "1")
        @NotNull(message = "adopterId is required")
        @Positive(message = "adopterId must be a positive number")
        Long adopterId,

        @Schema(description = "Defaults to the current time")
        @PastOrPresent(message = "adoptedAt cannot be in the future")
        Instant adoptedAt,

        @Schema(description = "Adoption fee in EUR", example = "85.00", defaultValue = "0.00")
        @DecimalMin(value = "0.00", message = "fee cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "fee must have at most 2 decimal places")
        BigDecimal fee,

        @Schema(example = "Adopter will pick it up on Saturday morning.")
        @Size(max = 500, message = "notes must be at most 500 characters")
        String notes) {
}
