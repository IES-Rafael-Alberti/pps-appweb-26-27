package com.vetclinic.security;

import com.vetclinic.auth.User;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues the short lived access tokens. Refresh tokens are opaque and handled by {@code AuthService}. */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.security.jwt.issuer:vet-clinic-api}") String issuer,
                      @Value("${app.security.jwt.access-token-ttl:15m}") Duration accessTokenTtl) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("role", user.getRole().name());
        if (user.getOwnerId() != null) {
            claims.claim("ownerId", user.getOwnerId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }
}
