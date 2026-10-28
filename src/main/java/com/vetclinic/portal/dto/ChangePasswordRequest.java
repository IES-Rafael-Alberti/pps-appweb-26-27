package com.vetclinic.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "ChangePasswordRequest")
public record ChangePasswordRequest(

        @Schema(example = "Cliente1234!")
        @NotBlank(message = "currentPassword is required")
        String currentPassword,

        @Schema(example = "NuevaClave1234!")
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 72, message = "newPassword must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$",
                message = "newPassword must contain at least one letter and one digit")
        String newPassword) {
}
