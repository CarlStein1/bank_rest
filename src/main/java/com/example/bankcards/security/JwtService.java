package com.example.bankcards.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final JwtParser jwtParser;
    private final long accessTokenExpirationMs;
    private final String issuer;

    public JwtService(
            @Value("${app.security.jwt.secret}")
            String secret,

            @Value("${app.security.jwt.access-token-expiration-ms}")
            long accessTokenExpirationMs,

            @Value("${app.security.jwt.issuer}")
            String issuer
    ) {
        if (accessTokenExpirationMs <= 0) {
            throw new IllegalArgumentException(
                    "Срок действия access-токена должен быть больше нуля"
            );
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException(
                    "Issuer JWT не должен быть пустым"
            );
        }

        byte[] keyBytes = Decoders.BASE64.decode(secret);

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.issuer = issuer;

        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build();
    }

    public String generateAccessToken(
            @NonNull UserDetails userDetails
    ) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusMillis(
                accessTokenExpirationMs
        );

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(issuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<String> extractValidUsername(
            @NonNull String token
    ) {
        try {
            Claims claims = jwtParser
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();

            if (username == null || username.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(username);

        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}