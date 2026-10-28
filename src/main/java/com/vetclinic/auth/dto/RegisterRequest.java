package com.vetclinic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "Self service sign up. Always creates a CLIENT account.")
public record RegisterRequest(

        @Schema(example = "Lucia Fernandez")
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @Schema(example = "lucia.fernandez@example.com")
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 160, message = "email must be at most 160 characters")
        String email,

        @Schema(example = "+34 600 111 222")
        @NotBlank(message = "phone is required")
        @Size(max = 30, message = "phone must be at most 30 characters")
        String phone,

        @Schema(description = "At least 8 characters, one letter and one digit.", example = "Cliente1234!")
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$",
                message = "password must contain at least one letter and one digit")
        String password) {
}
