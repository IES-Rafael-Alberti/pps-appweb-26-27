package com.vetclinic.auth.dto;

import com.vetclinic.auth.User;
import com.vetclinic.auth.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "UserResponse", description = "Public view of a login account. Never includes the password hash.")
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean enabled,
        Long ownerId,
        Instant createdAt,
        Instant lastLoginAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.getOwnerId(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }
}
