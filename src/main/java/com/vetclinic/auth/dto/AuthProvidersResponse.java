package com.vetclinic.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthProvidersResponse", description = "Which social login options are configured")
public record AuthProvidersResponse(
        boolean githubEnabled,
        String githubAuthorizationUrl) {
}
