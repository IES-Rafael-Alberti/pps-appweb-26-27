package com.vetclinic.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(name = "BookAppointmentRequest", description = "A client books a visit for one of their own pets.")
public record BookAppointmentRequest(

        @Schema(example = "3")
        @NotNull(message = "petId is required")
        @Positive(message = "petId must be a positive number")
        Long petId,

        @Schema(example = "2026-11-03T10:30:00")
        @NotNull(message = "scheduledAt is required")
        @Future(message = "scheduledAt must be in the future")
        LocalDateTime scheduledAt,

        @Schema(example = "Revisión anual")
        @NotBlank(message = "reason is required")
        @Size(max = 200, message = "reason must be at most 200 characters")
        String reason,

        @Schema(description = "Optional, defaults to 30 minutes.", example = "30")
        @Min(value = 5, message = "durationMinutes must be at least 5")
        @Max(value = 480, message = "durationMinutes must be at most 480")
        Integer durationMinutes,

        @Schema(example = "Prefiero por la mañana.")
        @Size(max = 1000, message = "notes must be at most 1000 characters")
        String notes) {
}
