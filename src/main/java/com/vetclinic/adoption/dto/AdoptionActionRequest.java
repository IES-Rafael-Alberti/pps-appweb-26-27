package com.vetclinic.adoption.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "AdoptionActionRequest", description = "Reason recorded when cancelling or returning an adoption")
public record AdoptionActionRequest(

        @Schema(example = "Adopter changed their mind and the animal stays in the shelter.")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason) {
}
