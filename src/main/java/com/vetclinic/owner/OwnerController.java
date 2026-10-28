package com.vetclinic.owner;

import com.vetclinic.common.PageResponse;
import com.vetclinic.owner.dto.OwnerRequest;
import com.vetclinic.owner.dto.OwnerResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owners")
@Tag(name = "Owners", description = "Clients of the clinic: pet owners and adopters")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping
    @Operation(summary = "List owners", description = "Paginated and sortable with `?page=0&size=20&sort=fullName,asc`")
    public PageResponse<OwnerResponse> list(
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(ownerService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an owner by id")
    public OwnerResponse get(@PathVariable Long id) {
        return ownerService.findById(id);
    }

    @GetMapping("/{phone}")
    @Operation(summary = "Get an owner by id")
    public OwnerResponse getByPhone(@PathVariable String phone) {
        return ownerService.findByPhone(phone);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new owner")
    public OwnerResponse create(@Valid @RequestBody OwnerRequest request) {
        return ownerService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an owner's details")
    public OwnerResponse update(@PathVariable Long id, @Valid @RequestBody OwnerRequest request) {
        return ownerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an owner", description = "Fails with 409 while the owner still has pets")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ownerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
