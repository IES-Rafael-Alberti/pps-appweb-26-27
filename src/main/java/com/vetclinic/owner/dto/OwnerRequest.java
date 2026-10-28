package com.vetclinic.owner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "OwnerRequest", description = "Payload to create or replace an owner")
public record OwnerRequest(

        @Schema(example = "Marta Ruiz")
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @Schema(example = "marta.ruiz@example.com")
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 160, message = "email must be at most 160 characters")
        String email,

        @Schema(example = "+34 600 123 456")
        @NotBlank(message = "phone is required")
        @Size(max = 30, message = "phone must be at most 30 characters")
        String phone,

        @Schema(example = "Calle Mayor 12, Madrid")
        @Size(max = 255, message = "address must be at most 255 characters")
        String address) {
}
