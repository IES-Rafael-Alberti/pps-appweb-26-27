package com.vetclinic.portal.dto;

import com.vetclinic.auth.Role;
import com.vetclinic.auth.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "MeResponse", description = "The logged in client and their clinic profile")
public record MeResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        Long ownerId,
        String phone,
        String address,
        Instant createdAt,
        Instant lastLoginAt) {

    public static MeResponse from(User user) {
        var owner = user.getOwner();
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getOwnerId(),
                owner == null ? null : owner.getPhone(),
                owner == null ? null : owner.getAddress(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }
}
