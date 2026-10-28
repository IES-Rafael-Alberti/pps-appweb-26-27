package com.vetclinic.portal;

import com.vetclinic.adoption.dto.AdoptionResponse;
import com.vetclinic.appointment.dto.AppointmentResponse;
import com.vetclinic.common.PageResponse;
import com.vetclinic.pet.dto.PetResponse;
import com.vetclinic.portal.dto.BookAppointmentRequest;
import com.vetclinic.portal.dto.ChangePasswordRequest;
import com.vetclinic.portal.dto.MeResponse;
import com.vetclinic.portal.dto.UpdateProfileRequest;
import com.vetclinic.sale.dto.SaleResponse;
import com.vetclinic.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client self service. These endpoints only ever return data belonging to the logged in owner, so the
 * client role never touches the staff listings.
 */
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client portal", description = "A client's own profile, pets, appointments, purchases and adoptions")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping
    @Operation(summary = "My profile")
    public MeResponse profile(@AuthenticationPrincipal CurrentUser currentUser) {
        return portalService.profile(currentUser);
    }

    @PutMapping
    @Operation(summary = "Update my contact details", description = "The email cannot be changed here.")
    public MeResponse updateProfile(@AuthenticationPrincipal CurrentUser currentUser,
                                    @Valid @RequestBody UpdateProfileRequest request) {
        return portalService.updateProfile(currentUser, request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change my password",
            description = "Revokes every refresh token: other devices must sign in again.")
    public void changePassword(@AuthenticationPrincipal CurrentUser currentUser,
                               @Valid @RequestBody ChangePasswordRequest request) {
        portalService.changePassword(currentUser, request);
    }

    @GetMapping("/pets")
    @Operation(summary = "My pets")
    public PageResponse<PetResponse> pets(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(portalService.pets(currentUser, pageable));
    }

    @GetMapping("/appointments")
    @Operation(summary = "My appointments, including those of every pet I own")
    public PageResponse<AppointmentResponse> appointments(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(portalService.appointments(currentUser, pageable));
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a visit for one of my pets")
    public AppointmentResponse bookAppointment(@AuthenticationPrincipal CurrentUser currentUser,
                                               @Valid @RequestBody BookAppointmentRequest request) {
        return portalService.bookAppointment(currentUser, request);
    }

    @PatchMapping("/appointments/{id}/cancel")
    @Operation(summary = "Cancel one of my appointments")
    public AppointmentResponse cancelAppointment(@AuthenticationPrincipal CurrentUser currentUser,
                                                 @PathVariable Long id) {
        return portalService.cancelAppointment(currentUser, id);
    }

    @GetMapping("/purchases")
    @Operation(summary = "My shop tickets")
    public PageResponse<SaleResponse> purchases(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PageableDefault(size = 20, sort = "soldAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(portalService.purchases(currentUser, pageable));
    }

    @GetMapping("/adoptions")
    @Operation(summary = "Adoptions I have made")
    public PageResponse<AdoptionResponse> adoptions(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PageableDefault(size = 20, sort = "adoptedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(portalService.adoptions(currentUser, pageable));
    }
}
