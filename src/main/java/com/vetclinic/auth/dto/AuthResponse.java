package com.vetclinic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "Access JWT plus a rotating refresh token.")
public record AuthResponse(

        @Schema(description = "Short lived JWT. Send it as 'Authorization: Bearer <token>'.")
        String accessToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds.", example = "900")
        long expiresIn,

        @Schema(description = "Opaque refresh token. Also set as an httpOnly cookie in browsers.")
        String refreshToken,

        @Schema(description = "Refresh token lifetime in seconds.", example = "604800")
        long refreshExpiresIn,

        UserResponse user) {

    public static AuthResponse of(String accessToken, long expiresIn, String refreshToken,
                                  long refreshExpiresIn, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, refreshToken, refreshExpiresIn, user);
    }
}
