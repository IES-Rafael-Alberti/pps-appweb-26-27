package com.vetclinic.auth;

import com.vetclinic.auth.dto.AuthProvidersResponse;
import com.vetclinic.auth.dto.AuthResponse;
import com.vetclinic.auth.dto.LoginRequest;
import com.vetclinic.auth.dto.RefreshRequest;
import com.vetclinic.auth.dto.RegisterRequest;
import com.vetclinic.auth.dto.UserResponse;
import com.vetclinic.security.CurrentUser;
import com.vetclinic.security.RefreshCookie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Access tokens are returned in the body; the refresh token is returned in the
 * body <em>and</em> as an httpOnly, SameSite=Lax cookie so a browser SPA never has to store it in JS.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, registration, OAuth2, token refresh and logout")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RefreshCookie refreshCookie;
    private final boolean githubEnabled;

    public AuthController(AuthService authService, UserService userService, RefreshCookie refreshCookie,
                          @Value("${app.security.oauth2.enabled:false}") boolean oauthEnabled,
                          @Value("${spring.security.oauth2.client.registration.github.client-id:}")
                          String githubClientId,
                          @Value("${spring.security.oauth2.client.registration.github.client-secret:}")
                          String githubClientSecret) {
        this.authService = authService;
        this.userService = userService;
        this.refreshCookie = refreshCookie;
        this.githubEnabled = oauthEnabled
                && !githubClientId.isBlank() && !"github-not-configured".equals(githubClientId)
                && !githubClientSecret.isBlank() && !"github-not-configured".equals(githubClientSecret);
    }

    @GetMapping("/providers")
    @Operation(summary = "Social login providers configured on this deployment")
    public AuthProvidersResponse providers() {
        return new AuthProvidersResponse(githubEnabled,
                githubEnabled ? "/oauth2/authorization/github" : null);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a client account",
            description = "Creates the login and, when the email is new, the clinic owner record. "
                    + "If an owner already exists with that email, the phone number must match.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.create(auth.refreshToken(),
                auth.refreshExpiresIn()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(auth);
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for an access token and a refresh token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.create(auth.refreshToken(),
                auth.refreshExpiresIn()).toString());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and get a new access token",
            description = "The token can be sent in the request body or left to the httpOnly cookie. "
                    + "Every successful call invalidates the previous refresh token.")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest body,
            @CookieValue(name = RefreshCookie.NAME, required = false) String cookieToken,
            HttpServletResponse response) {
        String token = body != null && body.refreshToken() != null && !body.refreshToken().isBlank()
                ? body.refreshToken()
                : cookieToken;
        AuthResponse auth = authService.refresh(token);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.create(auth.refreshToken(),
                auth.refreshExpiresIn()).toString());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke the refresh token and clear the cookie",
            description = "Idempotent: succeeds even if the token was already revoked or is missing.")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshRequest body,
            @CookieValue(name = RefreshCookie.NAME, required = false) String cookieToken,
            HttpServletResponse response) {
        String token = body != null && body.refreshToken() != null && !body.refreshToken().isBlank()
                ? body.refreshToken()
                : cookieToken;
        authService.logout(token);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Current account, taken from the Bearer token")
    public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return userService.findById(currentUser.id());
    }
}
