package com.vetclinic.adoption;

import com.vetclinic.adoption.dto.AdoptionActionRequest;
import com.vetclinic.adoption.dto.AdoptionResponse;
import com.vetclinic.adoption.dto.CreateAdoptionRequest;
import com.vetclinic.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/adoptions")
@Tag(name = "Adoptions", description = "Adoption of shelter animals")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdoptionController {

    private final AdoptionService adoptionService;

    public AdoptionController(AdoptionService adoptionService) {
        this.adoptionService = adoptionService;
    }

    @GetMapping
    @Operation(summary = "List adoptions", description = "Filter by pet, adopter, status or date range")
    public PageResponse<AdoptionResponse> list(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long adopterId,
            @Parameter(description = "completed, canceled, returned")
            @RequestParam(required = false) AdoptionStatus status,
            @Parameter(description = "ISO instant, e.g. 2026-04-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "adoptedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(
                adoptionService.search(petId, adopterId, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an adoption by id")
    public AdoptionResponse get(@PathVariable Long id) {
        return adoptionService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adopt an animal",
            description = "The pet must be 'available'. On success it becomes 'adopted' and its owner is set "
                    + "to the adopter. Fails with 409 otherwise.")
    public AdoptionResponse adopt(@Valid @RequestBody CreateAdoptionRequest request) {
        return adoptionService.adopt(request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an adoption", description = "The animal goes back to 'available'")
    public AdoptionResponse cancel(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) AdoptionActionRequest request) {
        return adoptionService.cancel(id, request);
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Register a returned animal", description = "The animal goes back to 'available'")
    public AdoptionResponse markAsReturned(@PathVariable Long id,
                                           @Valid @RequestBody(required = false) AdoptionActionRequest request) {
        return adoptionService.markAsReturned(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a closed adoption record", description = "Only canceled or returned ones")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adoptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
