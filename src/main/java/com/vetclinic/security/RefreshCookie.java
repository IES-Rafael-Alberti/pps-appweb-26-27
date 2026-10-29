package com.vetclinic.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the httpOnly refresh token cookie. Kept in one place because both the password login and the
 * OAuth2 success handler must issue exactly the same cookie.
 */
@Component
public class RefreshCookie {

    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/v1/auth";

    private final boolean secure;
    private final String sameSite;

    public RefreshCookie(@Value("${app.security.refresh-cookie-secure:false}") boolean secure,
                         @Value("${app.security.refresh-cookie-same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie create(String value, long maxAgeSeconds) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie clear() {
        return create("", 0);
    }

    /** Reads the cookie for clients that cannot use the request body (browser SPA). */
    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
