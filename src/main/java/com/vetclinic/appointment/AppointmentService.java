package com.vetclinic.appointment;

import com.vetclinic.appointment.dto.AppointmentRequest;
import com.vetclinic.appointment.dto.AppointmentResponse;
import com.vetclinic.common.BadRequestException;
import com.vetclinic.common.ConflictException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.pet.Pet;
import com.vetclinic.pet.PetRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppointmentService {

    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, PetRepository petRepository) {
        this.appointmentRepository = appointmentRepository;
        this.petRepository = petRepository;
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        return AppointmentResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> search(Long petId, AppointmentStatus status, LocalDateTime from,
                                            LocalDateTime to, Pageable pageable) {
        if (petId != null) {
            getPet(petId);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'.");
        }
        return appointmentRepository.search(petId, status, from, to, pageable).map(AppointmentResponse::from);
    }

    public AppointmentResponse create(AppointmentRequest request) {
        Pet pet = getPet(request.petId());
        Appointment appointment = new Appointment(pet, request.scheduledAt(), durationOf(request),
                request.reason().trim(), trim(request.vetName()), trim(request.notes()));
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = getEntity(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Appointment '%s' is '%s' and can no longer be edited. "
                    .formatted(id, appointment.getStatus().value())
                    + "Reschedule it back to 'scheduled' first.");
        }
        appointment.updateDetails(getPet(request.petId()), request.scheduledAt(), durationOf(request),
                request.reason().trim(), trim(request.vetName()), trim(request.notes()));
        return AppointmentResponse.from(appointment);
    }

    public AppointmentResponse changeStatus(Long id, AppointmentStatus target) {
        Appointment appointment = getEntity(id);
        AppointmentStatus current = appointment.getStatus();
        if (current == target) {
            throw new BadRequestException("Appointment '%s' is already '%s'."
                    .formatted(id, target.value()));
        }
        if (!current.canTransitionTo(target)) {
            throw new ConflictException("Cannot move an appointment from '%s' to '%s'. Allowed targets: %s"
                    .formatted(current.value(), target.value(), allowedTargets(current)));
        }
        if (target == AppointmentStatus.SCHEDULED && !appointment.getScheduledAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("This appointment's date is in the past. Reschedule it to a future "
                    + "date with PUT /api/v1/appointments/%d before re-activating it.".formatted(id));
        }
        appointment.changeStatus(target);
        return AppointmentResponse.from(appointment);
    }

    public void delete(Long id) {
        Appointment appointment = getEntity(id);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Completed appointments are part of the medical record and cannot be "
                    + "deleted (id %d).".formatted(id));
        }
        appointmentRepository.delete(appointment);
    }

    private String allowedTargets(AppointmentStatus current) {
        return AppointmentStatus.allowedTargets(current).stream()
                .map(AppointmentStatus::value)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
    }

    /**
     * An omitted {@code durationMinutes} arrives as {@code null} (the field is a boxed {@code Integer} so
     * that {@code @Min(5)} does not reject the absence of a value), and the documented default applies.
     */
    private int durationOf(AppointmentRequest request) {
        return request.durationMinutes() != null && request.durationMinutes() > 0
                ? request.durationMinutes()
                : DEFAULT_DURATION_MINUTES;
    }

    private Appointment getEntity(Long id) {
        return appointmentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Appointment", id));
    }

    private Pet getPet(Long petId) {
        return petRepository.findById(petId).orElseThrow(() -> NotFoundException.of("Pet", petId));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
