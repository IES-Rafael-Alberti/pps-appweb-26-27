package com.vetclinic.auth;

import com.vetclinic.auth.dto.AuthResponse;
import com.vetclinic.auth.dto.LoginRequest;
import com.vetclinic.auth.dto.RegisterRequest;
import com.vetclinic.auth.dto.UserResponse;
import com.vetclinic.common.ConflictException;
import com.vetclinic.common.UnauthorizedException;
import com.vetclinic.owner.Owner;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login, self service registration and the refresh token lifecycle.
 *
 * <p>Refresh tokens are opaque 256-bit values. Only their SHA-256 hash is persisted, each refresh rotates
 * the token and revokes the previous one, and presenting an already used token closes every session of
 * that account: that pattern is what a stolen token looks like.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTokenTtl;

    public AuthService(UserRepository userRepository,
                       OwnerRepository ownerRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.security.refresh-token-ttl:7d}") Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * Login or first-time registration through an external identity provider (GitHub). The provider email
     * is treated as verified: it links an existing client account or creates the owner record on the fly.
     * Staff accounts are never linked this way: they must keep using their clinic credentials.
     */
    @Transactional
    public AuthResponse loginWithOAuth2(String provider, String subject, String email, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByOauthProviderAndOauthSubject(provider, subject).orElse(null);

        if (user != null) {
            if (!user.isEnabled()) {
                throw new UnauthorizedException("This account has been disabled. Contact the clinic.");
            }
        } else {
            user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
            if (user != null) {
                if (user.getRole() != Role.CLIENT) {
                    throw new ConflictException(
                            "This email belongs to a staff account. Sign in with your clinic password.");
                }
                user.linkOAuth(provider, subject);
            } else {
                String name = fullName == null || fullName.isBlank() ? normalizedEmail : fullName.trim();
                Owner owner = ownerRepository.findByEmailIgnoreCase(normalizedEmail)
                        .orElseGet(() -> ownerRepository.save(new Owner(name, normalizedEmail, null, null)));
                user = new User(normalizedEmail, passwordEncoder.encode(newRefreshToken()), name,
                        Role.CLIENT, owner);
                user.linkOAuth(provider, subject);
                userRepository.save(user);
            }
        }

        user.registerLogin();
        return issueTokens(user);
    }

    /** Registers a client. If the clinic already has an owner with this email, the phone must match. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with email '%s' already exists".formatted(email));
        }

        Owner owner = ownerRepository.findByEmailIgnoreCase(email)
                .map(existing -> {
                    if (!samePhone(existing.getPhone(), request.phone())) {
                        throw new ConflictException(
                                "An owner with this email already exists. Contact the clinic to activate "
                                        + "your online account.");
                    }
                    return existing;
                })
                .orElseGet(() -> ownerRepository.save(new Owner(request.fullName().trim(), email,
                        request.phone().trim(), null)));

        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(),
                Role.CLIENT, owner);
        user.registerLogin();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> invalidCredentials());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedException("This account has been disabled. Contact the clinic.");
        }
        user.registerLogin();
        return issueTokens(user);
    }

    /**
     * Rotates the refresh token. {@code noRollbackFor} keeps the revocation of a reused token in the
     * database even though the request itself fails with 401.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("A refresh token is required.");
        }
        RefreshToken stored = refreshTokenRepository.findForUpdate(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));
        Instant now = Instant.now();

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), now);
            throw new UnauthorizedException(
                    "This refresh token has already been used. All sessions were closed as a precaution.");
        }
        if (stored.isExpired(now)) {
            throw new UnauthorizedException("The refresh token has expired. Sign in again.");
        }
        if (!stored.getUser().isEnabled()) {
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), now);
            throw new UnauthorizedException("This account has been disabled. Contact the clinic.");
        }

        stored.revoke(now);
        return issueTokens(stored.getUser());
    }

    /** Revokes the presented refresh token. Unknown or already revoked tokens are ignored (idempotent). */
    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findForUpdate(hash(rawToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private AuthResponse issueTokens(User user) {
        Instant now = Instant.now();
        String rawRefreshToken = newRefreshToken();
        refreshTokenRepository.save(new RefreshToken(hash(rawRefreshToken), user,
                now.plus(refreshTokenTtl)));

        String accessToken = jwtService.generateAccessToken(user);
        return AuthResponse.of(accessToken, jwtService.accessTokenTtl().toSeconds(), rawRefreshToken,
                refreshTokenTtl.toSeconds(), UserResponse.from(user));
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean samePhone(String stored, String provided) {
        return digits(stored).equals(digits(provided));
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid email or password.");
    }
}
