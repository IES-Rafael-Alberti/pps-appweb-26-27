package com.vetclinic.auth.dto;

import com.vetclinic.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "RoleRequest")
public record RoleRequest(

        @Schema(example = "manager")
        @NotNull(message = "role is required")
        Role role) {
}
