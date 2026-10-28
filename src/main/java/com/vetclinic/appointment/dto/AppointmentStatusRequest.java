package com.vetclinic.appointment.dto;

import com.vetclinic.appointment.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AppointmentStatusRequest", description = "New state for an appointment")
public record AppointmentStatusRequest(

        @Schema(description = "Accepted values: scheduled, completed, cancelled", example = "completed")
        @NotNull(message = "status is required")
        AppointmentStatus status) {
}
