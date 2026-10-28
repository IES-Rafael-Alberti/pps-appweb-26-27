package com.vetclinic.pet.dto;

import com.vetclinic.pet.Pet;
import com.vetclinic.pet.PetSex;
import com.vetclinic.pet.PetStatus;
import com.vetclinic.pet.Species;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

@Schema(name = "PetResponse")
public record PetResponse(
        Long id,
        String name,
        Species species,
        String breed,
        LocalDate birthDate,
        PetSex sex,
        PetStatus status,
        Long ownerId,
        String ownerName,
        String notes,
        long version,
        Instant createdAt) {

    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getBirthDate(),
                pet.getSex(),
                pet.getStatus(),
                pet.getOwner() == null ? null : pet.getOwner().getId(),
                pet.getOwner() == null ? null : pet.getOwner().getFullName(),
                pet.getNotes(),
                pet.getVersion(),
                pet.getCreatedAt());
    }
}
