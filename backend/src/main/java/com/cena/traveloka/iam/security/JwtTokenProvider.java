package com.cena.traveloka.iam.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * T046: JwtTokenProvider
 * Utility for generating and validating JWT tokens.
 *
 * Constitutional Compliance:
 * - NFR-002: JWT expiry 1 hour (configurable via environment)
 * - FR-003: Issue authentication tokens with 1-hour expiry
 * - Used by AuthenticationService for token generation
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final String issuer;
    private final JwtDecoder jwtDecoder;

    public JwtTokenProvider(
            @Value("${traveloka.iam.jwt.secret:traveloka-secret-key-minimum-256-bits-for-hs256-signature-algorithm}") String secret,
            @Value("${traveloka.iam.jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs,
            @Value("${traveloka.iam.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs,
            @Value("${traveloka.iam.jwt.issuer:traveloka-backend}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs; // Default: 1 hour (3600000ms)
        this.refreshTokenExpirationMs = refreshTokenExpirationMs; // Default: 7 days (604800000ms)
        this.issuer = issuer;
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Generate JWT access token from Authentication object.
     * Token includes userId, username, email, and roles.
     *
     * @param authentication Spring Security authentication object
     * @param userId User ID
     * @param email User email
     * @return JWT access token string
     */
    public String generateAccessToken(Authentication authentication, String userId, String email) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(accessTokenExpirationMs);

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userId)
                .claim("username", authentication.getName())
                .claim("email", email)
                .claim("roles", roles)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate JWT refresh token.
     * Refresh token has longer expiry (7 days) and minimal claims.
     *
     * @param userId User ID
     * @return JWT refresh token string
     */
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(refreshTokenExpirationMs);

        return Jwts.builder()
                .setSubject(userId)
                .claim("type", "refresh")
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Get user ID from JWT token.
     *
     * @param token JWT token string
     * @return User ID (subject)
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Get username from JWT token.
     *
     * @param token JWT token string
     * @return Username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("username", String.class);
    }

    /**
     * Get email from JWT token.
     *
     * @param token JWT token string
     * @return User email
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    /**
     * Get roles from JWT token.
     *
     * @param token JWT token string
     * @return List of role names
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("roles", List.class);
    }

    /**
     * Validate JWT token.
     * Checks signature, expiration, and issuer.
     *
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .requireIssuer(issuer)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if JWT token is expired.
     *
     * @param token JWT token string
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .requireIssuer(issuer)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            log.error("Error checking token expiration: {}", ex.getMessage());
            return true;
        }
    }

    /**
     * Decode JWT token using Spring Security JwtDecoder.
     * Used for integration with Spring Security OAuth2 Resource Server.
     *
     * @param token JWT token string
     * @return Decoded JWT
     */
    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    /**
     * Get token expiration time in milliseconds.
     *
     * @return Access token expiration in milliseconds
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    /**
     * Get token expiration time in seconds.
     *
     * @return Access token expiration in seconds
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}
