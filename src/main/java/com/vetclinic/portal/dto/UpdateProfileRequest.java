package com.vetclinic.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateProfileRequest", description = "A client can edit their own contact details, not the email.")
public record UpdateProfileRequest(

        @Schema(example = "Lucia Fernandez")
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @Schema(example = "+34 600 111 222")
        @NotBlank(message = "phone is required")
        @Size(max = 30, message = "phone must be at most 30 characters")
        String phone,

        @Schema(example = "Calle Sol 4, Madrid")
        @Size(max = 255, message = "address must be at most 255 characters")
        String address) {
}
