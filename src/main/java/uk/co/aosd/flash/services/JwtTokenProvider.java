package uk.co.aosd.flash.services;

import javax.crypto.SecretKey;

import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.co.aosd.flash.domain.UserRole;

/**
 * Service for JWT token generation and validation.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
        @Value("${app.jwt.secret:defaultSecretKeyForDevelopmentOnlyChangeInProduction12345678901234567890}") final String jwtSecret,
        @Value("${app.jwt.expiration-ms:86400000}") // 24 hours default
        final long jwtExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generate a JWT token for a user.
     *
     * @param userId
     *            the user ID
     * @param username
     *            the username
     * @param role
     *            the user role
     * @return JWT token string
     */
    public String generateToken(final UUID userId, final String username, final UserRole role) {
        final Date now = new Date();
        final Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("role", role.name())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token
     *            JWT token
     * @return user ID
     */
    public UUID getUserIdFromToken(final String token) {
        final Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract username from JWT token.
     *
     * @param token
     *            JWT token
     * @return username
     */
    public String getUsernameFromToken(final String token) {
        final Claims claims = getClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Extract user role from JWT token.
     *
     * @param token
     *            JWT token
     * @return user role
     */
    public UserRole getRoleFromToken(final String token) {
        final Claims claims = getClaimsFromToken(token);
        final String roleStr = claims.get("role", String.class);
        return UserRole.valueOf(roleStr);
    }

    /**
     * Validate JWT token.
     *
     * @param token
     *            JWT token
     * @return true if token is valid
     */
    public boolean validateToken(final String token) {
        try {
            final Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (final Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract claims from JWT token.
     *
     * @param token
     *            JWT token
     * @return claims
     */
    private Claims getClaimsFromToken(final String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
