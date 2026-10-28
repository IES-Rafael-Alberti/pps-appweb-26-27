package com.vetclinic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest")
public record LoginRequest(

        @Schema(example = "cliente@vetclinic.example")
        @NotBlank(message = "email is required")
        String email,

        @Schema(example = "Cliente1234!")
        @NotBlank(message = "password is required")
        String password) {
}
