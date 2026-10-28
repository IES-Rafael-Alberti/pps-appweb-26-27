package com.vetclinic.owner.dto;

import com.vetclinic.owner.Owner;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "OwnerResponse")
public record OwnerResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String address,
        Instant createdAt) {

    public static OwnerResponse from(Owner owner) {
        return new OwnerResponse(
                owner.getId(),
                owner.getFullName(),
                owner.getEmail(),
                owner.getPhone(),
                owner.getAddress(),
                owner.getCreatedAt());
    }
}
