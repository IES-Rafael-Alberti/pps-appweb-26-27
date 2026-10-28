package com.vetclinic.appointment.dto;

import com.vetclinic.appointment.Appointment;
import com.vetclinic.appointment.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;

@Schema(name = "AppointmentResponse")
public record AppointmentResponse(
        Long id,
        Long petId,
        String petName,
        LocalDateTime scheduledAt,
        int durationMinutes,
        String reason,
        String vetName,
        AppointmentStatus status,
        String notes,
        Instant createdAt) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPet().getId(),
                appointment.getPet().getName(),
                appointment.getScheduledAt(),
                appointment.getDurationMinutes(),
                appointment.getReason(),
                appointment.getVetName(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedAt());
    }
}
