package com.vetclinic.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(name = "AppointmentRequest", description = "Payload to book or reschedule an appointment")
public record AppointmentRequest(

        @Schema(example = "1")
        @NotNull(message = "petId is required")
        @Positive(message = "petId must be a positive number")
        Long petId,

        @Schema(description = "Local date and time, ISO-8601 without timezone.", example = "2026-04-02T10:30:00")
        @NotNull(message = "scheduledAt is required")
        @Future(message = "scheduledAt must be in the future")
        LocalDateTime scheduledAt,

        @Schema(example = "Annual check-up and vaccination")
        @NotBlank(message = "reason is required")
        @Size(max = 200, message = "reason must be at most 200 characters")
        String reason,

        @Schema(description = "Optional veterinarian in charge", example = "Dra. Elena Vazquez")
        @Size(max = 120, message = "vetName must be at most 120 characters")
        String vetName,

        @Schema(description = "Length of the visit in minutes. Optional: defaults to 30 when omitted.",
                example = "30", defaultValue = "30")
        @Min(value = 5, message = "durationMinutes must be at least 5")
        @Max(value = 480, message = "durationMinutes must be at most 480")
        Integer durationMinutes,

        @Schema(example = "Bring the vaccination record.")
        @Size(max = 1000, message = "notes must be at most 1000 characters")
        String notes) {
}
