package com.vetclinic.appointment;

import com.vetclinic.appointment.dto.AppointmentRequest;
import com.vetclinic.appointment.dto.AppointmentResponse;
import com.vetclinic.appointment.dto.AppointmentStatusRequest;
import com.vetclinic.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Veterinary visits")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @Operation(summary = "List appointments",
            description = "Filter by pet, status or a time window. Defaults to `scheduledAt` ascending.")
    public PageResponse<AppointmentResponse> list(
            @RequestParam(required = false) Long petId,
            @Parameter(description = "scheduled, completed, cancelled")
            @RequestParam(required = false) AppointmentStatus status,
            @Parameter(description = "ISO local date-time, e.g. 2026-04-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(appointmentService.search(petId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an appointment by id")
    public AppointmentResponse get(@PathVariable Long id) {
        return appointmentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book an appointment")
    public AppointmentResponse create(@Valid @RequestBody AppointmentRequest request) {
        return appointmentService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reschedule or edit a scheduled appointment")
    public AppointmentResponse update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
        return appointmentService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change the appointment state",
            description = "scheduled -> completed | cancelled, cancelled -> scheduled. Completed is final.")
    public AppointmentResponse changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody AppointmentStatusRequest request) {
        return appointmentService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an appointment", description = "Completed visits cannot be deleted")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
