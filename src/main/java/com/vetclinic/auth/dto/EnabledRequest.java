package com.vetclinic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "EnabledRequest")
public record EnabledRequest(

        @Schema(example = "false")
        @NotNull(message = "enabled is required")
        Boolean enabled) {
}
