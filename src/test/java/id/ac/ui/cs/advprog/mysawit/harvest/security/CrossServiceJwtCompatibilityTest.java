package id.ac.ui.cs.advprog.mysawit.harvest.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthenticationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrossServiceJwtCompatibilityTest {

    static final String SHARED_DEV_SECRET = "dev-secret-key-dev-secret-key-123456";

    private static final SecretKey SHARED_KEY =
            Keys.hmacShaKeyFor(SHARED_DEV_SECRET.getBytes(StandardCharsets.UTF_8));

    private HarvestJwtClaimsResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new HarvestJwtClaimsResolver(SHARED_DEV_SECRET);
    }

    @Test
    void resolve_authMintedBuruhToken_returnsCorrectSubmissionContext() {
        String buruhId = UUID.randomUUID().toString();
        String bearer = authBearer(Map.of(
                "sub", buruhId,
                "email", "buruh@example.com",
                "role", "BURUH",
                "username", "buruhUser",
                "nama", "Budi Worker"));

        HarvestSubmissionContext ctx = resolver.resolve(bearer);

        assertEquals(buruhId, ctx.buruhId());
        assertEquals("BURUH", ctx.role().toUpperCase());
        assertEquals("Budi Worker", ctx.buruhName());
    }

    @Test
    void resolve_authMintedBuruhToken_withoutNama_fallsBackToUsername() {
        String buruhId = UUID.randomUUID().toString();
        String bearer = authBearer(Map.of(
                "sub", buruhId,
                "role", "BURUH",
                "username", "buruhUser"));

        HarvestSubmissionContext ctx = resolver.resolve(bearer);
        assertEquals("buruhUser", ctx.buruhName());
    }

    @Test
    void resolve_authMintedNonBuruhToken_throwsForbidden() {
        String bearer = authBearer(Map.of(
                "sub", UUID.randomUUID().toString(),
                "role", "MANDOR",
                "username", "mandorUser",
                "nama", "Mandor User"));

        assertThrows(HarvestAuthorizationException.class, () -> resolver.resolve(bearer));
    }

    @Test
    void resolveMandor_authMintedMandorToken_returnsCorrectReviewerContext() {
        String mandorId = UUID.randomUUID().toString();
        String bearer = authBearer(Map.of(
                "sub", mandorId,
                "email", "mandor@example.com",
                "role", "MANDOR",
                "username", "mandorUser",
                "nama", "Siti Mandor"));

        HarvestReviewerContext ctx = resolver.resolveMandor(bearer);

        assertEquals(mandorId, ctx.userId());
        assertEquals("MANDOR", ctx.role().toUpperCase());
        assertEquals("Siti Mandor", ctx.name());
    }

    @Test
    void resolveMandor_authMintedBuruhToken_throwsForbidden() {
        String bearer = authBearer(Map.of(
                "sub", UUID.randomUUID().toString(),
                "role", "BURUH",
                "username", "buruhUser",
                "nama", "Budi Worker"));

        assertThrows(HarvestAuthorizationException.class,
                () -> resolver.resolveMandor(bearer));
    }

    @Test
    void resolveViewer_authMintedAdminToken_returnsCorrectViewerContext() {
        String adminId = UUID.randomUUID().toString();
        String bearer = authBearer(Map.of(
                "sub", adminId,
                "role", "ADMIN",
                "username", "adminUser",
                "nama", "Admin User"));

        HarvestViewerContext ctx = resolver.resolveViewer(bearer);

        assertEquals(adminId, ctx.userId());
        assertEquals("ADMIN", ctx.role().toUpperCase());
    }

    @Test
    void resolveViewer_authMintedSupirToken_throwsForbidden() {
        String bearer = authBearer(Map.of(
                "sub", UUID.randomUUID().toString(),
                "role", "SUPIR",
                "username", "supirUser",
                "nama", "Supir User"));

        assertThrows(HarvestAuthorizationException.class,
                () -> resolver.resolveViewer(bearer));
    }

    @Test
    void resolve_tokenSignedWithWrongSecret_throwsAuthentication() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "completely-different-secret-at-least-32chars"
                        .getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        String token = Jwts.builder()
                .claims(Map.of("sub", UUID.randomUUID().toString(), "role", "BURUH"))
                .issuedAt(new Date(now))
                .expiration(new Date(now + 3_600_000L))
                .signWith(wrongKey)
                .compact();

        assertThrows(HarvestAuthenticationException.class,
                () -> resolver.resolve("Bearer " + token));
    }

    @Test
    void resolve_missingAuthorizationHeader_throwsAuthentication() {
        assertThrows(HarvestAuthenticationException.class, () -> resolver.resolve(null));
    }

    private static String authBearer(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        String compact = Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + 3_600_000L))
                .signWith(SHARED_KEY)
                .compact();
        return "Bearer " + compact;
    }
}
