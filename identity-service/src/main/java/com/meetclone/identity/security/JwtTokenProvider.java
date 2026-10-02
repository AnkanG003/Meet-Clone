package com.meetclone.identity.security;

import com.meetclone.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessTokenTtlMillis;
    private final long refreshTokenTtlMillis;

    @Autowired
    public JwtTokenProvider(
            @Value("${app.jwt.access-secret}") String accessSecret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays
    ) {
        this.accessSecretKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMillis = accessTokenTtlMinutes * 60 * 1000L;
        this.refreshTokenTtlMillis = refreshTokenTtlDays * 24 * 60 * 60 * 1000L;
    }

    // Overload for tests / backwards compatibility
    public JwtTokenProvider(String secret, long accessTokenTtlMinutes, long refreshTokenTtlDays) {
        this(secret, secret, accessTokenTtlMinutes, refreshTokenTtlDays);
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenTtlMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(accessSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenTtlMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(refreshSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(accessSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return "ACCESS".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(refreshSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return "REFRESH".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID getUserIdFromAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(accessSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public UUID getUserIdFromRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    @Deprecated
    public boolean validate(String token) {
        return validateAccessToken(token);
    }

    @Deprecated
    public UUID getUserId(String token) {
        return getUserIdFromAccessToken(token);
    }

    public PublicKey getPublicKey() {
        return null;
    }
}
