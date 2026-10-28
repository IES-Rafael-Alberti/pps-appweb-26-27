package com.vetclinic.pet.dto;

import com.vetclinic.pet.PetSex;
import com.vetclinic.pet.PetStatus;
import com.vetclinic.pet.Species;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(name = "PetRequest", description = "Payload to register or update a pet")
public record PetRequest(

        @Schema(example = "Roco")
        @NotBlank(message = "name is required")
        @Size(max = 80, message = "name must be at most 80 characters")
        String name,

        @Schema(description = "Accepted values: dog, cat, bird, rabbit, reptile, other", example = "dog")
        @NotNull(message = "species is required")
        Species species,

        @Schema(example = "Schnauzer")
        @Size(max = 80, message = "breed must be at most 80 characters")
        String breed,

        @Schema(example = "2019-04-02")
        @PastOrPresent(message = "birthDate cannot be in the future")
        LocalDate birthDate,

        @Schema(description = "Accepted values: male, female, unknown", example = "male")
        @NotNull(message = "sex is required")
        PetSex sex,

        @Schema(description = """
                Optional. When omitted it is derived from ownerId: 'owned' if an owner is given, \
                'available' otherwise. 'adopted' is rejected here because only POST /api/v1/adoptions can set it.""",
                example = "owned", allowableValues = {"available", "owned"})
        PetStatus status,

        @Schema(description = "Id of the client that owns this pet. Required when status is 'owned'.", example = "1")
        @Positive(message = "ownerId must be a positive number")
        Long ownerId,

        @Schema(example = "Shy with strangers, vaccinated and microchipped.")
        @Size(max = 500, message = "notes must be at most 500 characters")
        String notes) {
}
