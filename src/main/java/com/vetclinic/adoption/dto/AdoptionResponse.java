package com.vetclinic.adoption.dto;

import com.vetclinic.adoption.Adoption;
import com.vetclinic.adoption.AdoptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "AdoptionResponse")
public record AdoptionResponse(
        Long id,
        Long petId,
        String petName,
        String petSpecies,
        Long adopterId,
        String adopterName,
        Instant adoptedAt,
        AdoptionStatus status,
        BigDecimal fee,
        String notes,
        long version) {

    public static AdoptionResponse from(Adoption adoption) {
        return new AdoptionResponse(
                adoption.getId(),
                adoption.getPet().getId(),
                adoption.getPet().getName(),
                adoption.getPet().getSpecies().value(),
                adoption.getAdopter().getId(),
                adoption.getAdopter().getFullName(),
                adoption.getAdoptedAt(),
                adoption.getStatus(),
                adoption.getFee(),
                adoption.getNotes(),
                adoption.getVersion());
    }
}
