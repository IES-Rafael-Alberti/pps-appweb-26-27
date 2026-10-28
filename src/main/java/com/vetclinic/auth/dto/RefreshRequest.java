package com.vetclinic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optional body for {@code POST /api/v1/auth/refresh} and {@code /auth/logout}. Browsers can rely on the
 * httpOnly {@code refresh_token} cookie instead, in which case the body may be omitted.
 */
@Schema(name = "RefreshRequest", description = "Optional: the refresh token can also travel in the httpOnly cookie.")
public record RefreshRequest(

        @Schema(description = "Opaque refresh token returned by login/register/refresh.")
        String refreshToken) {
}
