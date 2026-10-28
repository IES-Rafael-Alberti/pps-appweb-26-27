package com.vetclinic.pet;

import com.vetclinic.common.PageResponse;
import com.vetclinic.pet.dto.PetRequest;
import com.vetclinic.pet.dto.PetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pets")
@Tag(name = "Pets", description = "Client pets and shelter animals available for adoption")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    @Operation(summary = "Search pets",
            description = "Every filter is optional and can be combined. "
                    + "Set `shelter=true` for animals waiting for adoption.")
    public PageResponse<PetResponse> search(
            @Parameter(description = "dog, cat, bird, rabbit, reptile, other")
            @RequestParam(required = false) Species species,
            @Parameter(description = "available, owned, adopted")
            @RequestParam(required = false) PetStatus status,
            @RequestParam(required = false) Long ownerId,
            @Parameter(description = "true when the animal has no owner yet")
            @RequestParam(required = false) Boolean shelter,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(petService.search(species, status, ownerId, shelter, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pet by id")
    public PetResponse get(@PathVariable Long id) {
        return petService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a pet",
            description = "Without `ownerId` the pet is registered as a shelter animal available for adoption.")
    public PetResponse create(@Valid @RequestBody PetRequest request) {
        return petService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pet")
    public PetResponse update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        return petService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pet", description = "Fails with 409 while the pet has appointments or adoptions")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        petService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
