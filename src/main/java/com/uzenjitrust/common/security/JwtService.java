package com.uzenjitrust.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public JwtToken issueToken(UUID userId, Set<AppRole> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.getTtlSeconds());
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .id(tokenId)
                .issuer(properties.getIssuer())
                .subject(userId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("roles", roles.stream().map(Enum::name).toList())
                .signWith(secretKey)
                .compact();

        return new JwtToken(token, tokenId, issuedAt, expiresAt);
    }

    public ParsedJwt parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        @SuppressWarnings("unchecked")
        Set<AppRole> roles = ((java.util.List<String>) claims.get("roles")).stream()
                .map(AppRole::valueOf)
                .collect(Collectors.toSet());
        return new ParsedJwt(
                new ActorPrincipal(userId, roles),
                claims.getId(),
                claims.getExpiration().toInstant()
        );
    }

    public record JwtToken(String token, String tokenId, Instant issuedAt, Instant expiresAt) {
    }

    public record ParsedJwt(ActorPrincipal actor, String tokenId, Instant expiresAt) {
    }
}
