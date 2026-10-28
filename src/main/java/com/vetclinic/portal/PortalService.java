package com.vetclinic.portal;

import com.vetclinic.adoption.AdoptionRepository;
import com.vetclinic.adoption.dto.AdoptionResponse;
import com.vetclinic.appointment.Appointment;
import com.vetclinic.appointment.AppointmentRepository;
import com.vetclinic.appointment.AppointmentService;
import com.vetclinic.appointment.AppointmentStatus;
import com.vetclinic.appointment.dto.AppointmentRequest;
import com.vetclinic.appointment.dto.AppointmentResponse;
import com.vetclinic.auth.User;
import com.vetclinic.auth.UserRepository;
import com.vetclinic.auth.UserService;
import com.vetclinic.common.ForbiddenException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.owner.Owner;
import com.vetclinic.pet.Pet;
import com.vetclinic.pet.PetRepository;
import com.vetclinic.pet.dto.PetResponse;
import com.vetclinic.portal.dto.BookAppointmentRequest;
import com.vetclinic.portal.dto.ChangePasswordRequest;
import com.vetclinic.portal.dto.MeResponse;
import com.vetclinic.portal.dto.UpdateProfileRequest;
import com.vetclinic.sale.FoodSaleRepository;
import com.vetclinic.sale.dto.SaleResponse;
import com.vetclinic.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The client portal: every query is scoped to the owner record linked to the logged in account. Requests
 * for somebody else's pet or appointment answer 404, not 403, so ids cannot be probed.
 */
@Service
@Transactional
public class PortalService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;
    private final FoodSaleRepository foodSaleRepository;
    private final AdoptionRepository adoptionRepository;
    private final AppointmentService appointmentService;
    private final UserService userService;

    public PortalService(UserRepository userRepository,
                         PetRepository petRepository,
                         AppointmentRepository appointmentRepository,
                         FoodSaleRepository foodSaleRepository,
                         AdoptionRepository adoptionRepository,
                         AppointmentService appointmentService,
                         UserService userService) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.appointmentRepository = appointmentRepository;
        this.foodSaleRepository = foodSaleRepository;
        this.adoptionRepository = adoptionRepository;
        this.appointmentService = appointmentService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public MeResponse profile(CurrentUser currentUser) {
        return MeResponse.from(requireOwnerAccount(currentUser));
    }

    public MeResponse updateProfile(CurrentUser currentUser, UpdateProfileRequest request) {
        User user = requireOwnerAccount(currentUser);
        Owner owner = user.getOwner();
        user.updateFullName(request.fullName().trim());
        owner.updateDetails(request.fullName().trim(), owner.getEmail(), request.phone().trim(),
                trim(request.address()));
        return MeResponse.from(user);
    }

    @Transactional(readOnly = true)
    public Page<PetResponse> pets(CurrentUser currentUser, Pageable pageable) {
        Long ownerId = ownerId(currentUser);
        return petRepository.search(null, null, ownerId, null, pageable).map(PetResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> appointments(CurrentUser currentUser, Pageable pageable) {
        Long ownerId = ownerId(currentUser);
        return appointmentRepository.findByOwnerId(ownerId, pageable).map(AppointmentResponse::from);
    }

    public AppointmentResponse bookAppointment(CurrentUser currentUser, BookAppointmentRequest request) {
        Long ownerId = ownerId(currentUser);
        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> NotFoundException.of("Pet", request.petId()));
        if (pet.getOwner() == null || !pet.getOwner().getId().equals(ownerId)) {
            throw NotFoundException.of("Pet", request.petId());
        }
        return appointmentService.create(new AppointmentRequest(pet.getId(), request.scheduledAt(),
                request.reason(), null, request.durationMinutes(), request.notes()));
    }

    public AppointmentResponse cancelAppointment(CurrentUser currentUser, Long appointmentId) {
        Long ownerId = ownerId(currentUser);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> NotFoundException.of("Appointment", appointmentId));
        if (appointment.getPet().getOwner() == null
                || !appointment.getPet().getOwner().getId().equals(ownerId)) {
            throw NotFoundException.of("Appointment", appointmentId);
        }
        return appointmentService.changeStatus(appointmentId, AppointmentStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> purchases(CurrentUser currentUser, Pageable pageable) {
        Long ownerId = ownerId(currentUser);
        return foodSaleRepository.search(ownerId, null, null, null, pageable).map(SaleResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AdoptionResponse> adoptions(CurrentUser currentUser, Pageable pageable) {
        Long ownerId = ownerId(currentUser);
        return adoptionRepository.search(null, ownerId, null, null, null, pageable)
                .map(AdoptionResponse::from);
    }

    public void changePassword(CurrentUser currentUser, ChangePasswordRequest request) {
        // Delegated to UserService, which revokes every refresh token after a password change.
        userService.changePassword(currentUser.id(), request.currentPassword(), request.newPassword());
    }

    private Long ownerId(CurrentUser currentUser) {
        return requireOwnerAccount(currentUser).getOwnerId();
    }

    private User requireOwnerAccount(CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ForbiddenException("The session is no longer valid."));
        if (user.getOwner() == null) {
            throw new ForbiddenException("This account is not linked to a clinic client profile.");
        }
        return user;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
