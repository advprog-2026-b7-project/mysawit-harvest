package id.ac.ui.cs.advprog.mysawit.harvest.security;

import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthenticationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import java.nio.charset.StandardCharsets;

@Component
public class HarvestJwtClaimsResolver {

    private final SecretKey secretKey;

    public HarvestJwtClaimsResolver(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public HarvestSubmissionContext resolve(String authorizationHeader) {
        Map<String, Object> claims = decodeClaims(authorizationHeader);

        String role = firstNonBlank(
                asString(claims.get("role")),
                asString(claims.get("roles")));

        if (role == null || !role.equalsIgnoreCase("BURUH")) {
            throw new HarvestAuthorizationException(HarvestErrorKey.FORBIDDEN,
                    "Caller does not have the BURUH role");
        }

        String buruhId = extractUserId(claims);

        if (buruhId == null || buruhId.isBlank()) {
            throw new HarvestAuthenticationException("JWT is missing a subject/user identifier");
        }

        String plantationId = firstNonBlank(
                asString(claims.get("plantationId")),
                asString(claims.get("plantation_id")));

        String buruhName = firstNonBlank(
                asString(claims.get("name")),
                asString(claims.get("fullName")),
                asString(claims.get("buruhName")));

        return new HarvestSubmissionContext(buruhId, buruhName, plantationId, role);
    }

    public HarvestViewerContext resolveViewer(String authorizationHeader) {
        Map<String, Object> claims = decodeClaims(authorizationHeader);

        String role = firstNonBlank(
                asString(claims.get("role")),
                asString(claims.get("roles")));

        if (role == null
                || (!role.equalsIgnoreCase("BURUH")
                && !role.equalsIgnoreCase("MANDOR")
                && !role.equalsIgnoreCase("ADMIN"))) {
            throw new HarvestAuthorizationException(HarvestErrorKey.FORBIDDEN,
                    "Caller role is not allowed for this endpoint");
        }

        String userId = extractUserId(claims);
        if (userId == null || userId.isBlank()) {
            throw new HarvestAuthenticationException("JWT is missing a subject/user identifier");
        }

        return new HarvestViewerContext(userId, role.toUpperCase());
    }

    public HarvestReviewerContext resolveMandor(String authorizationHeader) {
        Map<String, Object> claims = decodeClaims(authorizationHeader);

        String role = firstNonBlank(
                asString(claims.get("role")),
                asString(claims.get("roles")));

        if (role == null || !role.equalsIgnoreCase("MANDOR")) {
            throw new HarvestAuthorizationException(HarvestErrorKey.FORBIDDEN,
                    "Caller does not have the MANDOR role");
        }

        String userId = extractUserId(claims);
        if (userId == null || userId.isBlank()) {
            throw new HarvestAuthenticationException("JWT is missing a subject/user identifier");
        }

        String name = firstNonBlank(
                asString(claims.get("name")),
                asString(claims.get("fullName")),
                asString(claims.get("mandorName")));

        return new HarvestReviewerContext(userId, role.toUpperCase(), name);
    }

    private Map<String, Object> decodeClaims(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new HarvestAuthenticationException("Authorization header is required");
        }

        String token = extractToken(authorizationHeader);
        return parseSignedClaims(token);
    }

    private String extractUserId(Map<String, Object> claims) {
        return firstNonBlank(
                asString(claims.get("sub")),
                asString(claims.get("userId")),
                asString(claims.get("buruhId")),
                asString(claims.get("mandorId")),
                asString(claims.get("adminId")));
    }

    private String extractToken(String authorizationHeader) {
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new HarvestAuthenticationException("Authorization header must use Bearer scheme");
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }

    private Map<String, Object> parseSignedClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new HarvestAuthenticationException("Missing or invalid JWT token");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String stringValue) {
            return stringValue;
        }

        if (value instanceof List<?> listValue && !listValue.isEmpty()) {
            Object first = listValue.get(0);
            return first == null ? null : String.valueOf(first);
        }

        return String.valueOf(value);
    }
}
