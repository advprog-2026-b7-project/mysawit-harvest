package id.ac.ui.cs.advprog.mysawit.harvest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthenticationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HarvestJwtClaimsResolverTest {

    private static final String SECRET = "test-local-secret-for-harvest-change-me!!";

    private HarvestJwtClaimsResolver resolver;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        resolver = new HarvestJwtClaimsResolver(SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void resolveMandor_shouldReadVerifiedSignedTokenClaims() {
        String token = Jwts.builder()
                .subject("mandor-1")
                .claim("role", "MANDOR")
                .claim("name", "Budi Santoso")
                .signWith(secretKey)
                .compact();

        HarvestReviewerContext context = resolver.resolveMandor("Bearer " + token);

        assertEquals("mandor-1", context.userId());
        assertEquals("MANDOR", context.role());
        assertEquals("Budi Santoso", context.name());
    }

    @Test
    void resolveViewer_shouldRejectTamperedToken() {
        String token = Jwts.builder()
                .subject("admin-1")
                .claim("role", "ADMIN")
                .signWith(secretKey)
                .compact();
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertThrows(
                HarvestAuthenticationException.class,
                () -> resolver.resolveViewer("Bearer " + tamperedToken)
        );
    }

    @Test
    void resolveSubmission_shouldRejectMissingBearerScheme() {
        assertThrows(
                HarvestAuthenticationException.class,
                () -> resolver.resolve("not-a-bearer-token")
        );
    }
}
