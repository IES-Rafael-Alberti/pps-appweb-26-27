package com.vetclinic.auth;

import com.vetclinic.auth.dto.CreateUserRequest;
import com.vetclinic.auth.dto.EnabledRequest;
import com.vetclinic.auth.dto.RoleRequest;
import com.vetclinic.auth.dto.UserResponse;
import com.vetclinic.common.PageResponse;
import com.vetclinic.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * User administration. Only the administrator role can reach these endpoints, both at the URL level
 * (SecurityConfig) and here. Managers can run the clinic but cannot hand out accounts or roles.
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users", description = "Admin only: staff and client accounts")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List accounts", description = "Filters: `role`, `enabled`, `q` (name or email).")
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(userService.search(role, enabled, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an account by id")
    public UserResponse get(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a staff account, or a client account linked to an owner")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change the role", description = "The last active administrator cannot be demoted.")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request,
                                   @AuthenticationPrincipal CurrentUser actor) {
        return userService.changeRole(id, request.role(), actor);
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Enable or disable an account",
            description = "Disabling revokes the refresh tokens immediately; access tokens stop working on "
                    + "the next request.")
    public UserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody EnabledRequest request,
                                   @AuthenticationPrincipal CurrentUser actor) {
        return userService.setEnabled(id, request.enabled(), actor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an account", description = "The linked owner record and its history are kept.")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CurrentUser actor) {
        userService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
