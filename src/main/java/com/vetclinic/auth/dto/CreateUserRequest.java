package com.vetclinic.auth.dto;

import com.vetclinic.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateUserRequest", description = "Admin only: create a staff or client account.")
public record CreateUserRequest(

        @Schema(example = "Marta Ruiz")
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @Schema(example = "marta.ruiz@vetclinic.example")
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 160, message = "email must be at most 160 characters")
        String email,

        @Schema(example = "Temporal1234!")
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$",
                message = "password must contain at least one letter and one digit")
        String password,

        @Schema(example = "manager")
        @NotNull(message = "role is required")
        Role role,

        @Schema(description = "Required for the client role: the clinic owner record behind the account.",
                example = "1")
        Long ownerId) {
}
