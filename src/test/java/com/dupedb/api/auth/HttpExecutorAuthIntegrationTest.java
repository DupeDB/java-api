package com.dupedb.api.auth;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for D-13 + D-14:
 * <ul>
 *   <li>Wave 6 plan 103-11 ships REAL AuthManager (single-flight refresh,
 *       forceRefresh, fallback).</li>
 *   <li>Wave 7 plan 103-12 ships REAL HttpExecutor (Bearer transport +
 *       401 retry hook).</li>
 * </ul>
 *
 * <p>This test wires BOTH real components together against a single
 * in-process {@link HttpServer} that mocks both the {@code /api/oauth/token}
 * (refresh) endpoint AND a resource endpoint that issues 401 once with the
 * stale token and 200 with the rotated token.
 *
 * <p>Acceptance gate (closes the seam left by HttpExecutorRetryTest's stub
 * AuthManager and AuthManagerTest's missing HttpExecutor):
 * <ul>
 *   <li>Exactly ONE POST to {@code /api/oauth/token} (single-flight refresh
 *       — D-13 invariant).</li>
 *   <li>Resource endpoint observes {@code Bearer OLD_TOKEN} on the first hit,
 *       {@code Bearer NEW_TOKEN} on the retry.</li>
 *   <li>{@link HttpExecutor#get(String, Class)} returns success without throwing.</li>
 * </ul>
 */
class HttpExecutorAuthIntegrationTest {

    private HttpServer server;
    private int port;
    private final AtomicInteger refreshHits = new AtomicInteger(0);
    private final AtomicInteger resourceHits = new AtomicInteger(0);
    private final AtomicReference<String> firstResourceAuth = new AtomicReference<>();
    private final AtomicReference<String> secondResourceAuth = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        refreshHits.set(0);
        resourceHits.set(0);
        firstResourceAuth.set(null);
        secondResourceAuth.set(null);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        // Mock /api/oauth/token — succeeds with rotated tokens.
        server.createContext("/api/oauth/token", exchange -> {
            refreshHits.incrementAndGet();
            String json = "{\"access_token\":\"NEW_TOKEN\",\"token_type\":\"Bearer\","
                + "\"expires_in\":3600,\"refresh_token\":\"NEW_REFRESH\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        // Mock resource endpoint — first hit with OLD_TOKEN returns 401 +
        // WWW-Authenticate invalid_token; subsequent hits return 200.
        server.createContext("/test", exchange -> {
            int n = resourceHits.incrementAndGet();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (n == 1) {
                firstResourceAuth.set(auth);
                exchange.getResponseHeaders().add("WWW-Authenticate",
                    "Bearer realm=\"api\", error=\"invalid_token\"");
                byte[] body = "{\"error\":\"invalid_token\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(401, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
            } else {
                secondResourceAuth.set(auth);
                String json = "{\"ok\":true}";
                byte[] body = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
            }
        });

        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void realAuthManager_realHttpExecutor_refreshesAndRetriesWithRotatedBearer(@TempDir Path tempDir)
            throws Exception {
        String baseUrl = "http://127.0.0.1:" + port;

        // Real OAuthFlow (we will not invoke its browser flow — AuthManager
        // only calls authenticateAndExchange on D-15 invalid_grant, which we
        // do not trigger here).
        OAuthFlow flow = new OAuthFlow(baseUrl, "test-app");

        // Real TokenStore primed with creds whose expiresAt is FUTURE so
        // AuthManager.getToken() returns OLD_TOKEN without proactively
        // refreshing. The 401 from the resource endpoint then drives
        // HttpExecutor to call forceRefresh(), which exercises the REAL
        // postRefresh path against the mock /api/oauth/token endpoint.
        Path tokenFile = tempDir.resolve("creds.json");
        TokenStore store = new TokenStore(tokenFile);
        store.save(new Credentials(
            "OLD_TOKEN",
            "test-app",
            Instant.now().toString(),
            "OLD_REFRESH",
            Instant.now().plusSeconds(3600)              // FUTURE — bypass proactive refresh
        ));

        // Real AuthManager — real refreshLock, real postRefresh(...) network path.
        AuthManager authManager = new AuthManager(store, flow);

        // Real HttpExecutor wired to authManager::getToken (Supplier<String>
        // contract preserved from Phase 99) AND given the AuthManager so the
        // 3-arg ctor enables the retry path. AuthManager.getToken() declares
        // a checked exception, so wrap in RuntimeException for Supplier.
        Supplier<String> tokenSupplier = () -> {
            try {
                return authManager.getToken();
            } catch (DupeDBException e) {
                throw new RuntimeException(e);
            }
        };
        HttpExecutor exec = new HttpExecutor(baseUrl, tokenSupplier, authManager);

        // Exercise the seam.
        java.util.Map<?, ?> result = exec.get("/test", java.util.Map.class);
        assertNotNull(result, "resource endpoint should return a parsed body");
        assertEquals(true, result.get("ok"),
            "second hit should succeed with the rotated token");

        // D-13 invariant: exactly ONE refresh under the lock.
        assertEquals(1, refreshHits.get(),
            "D-13: exactly one POST to /api/oauth/token under the single-flight refresh lock");

        // D-14 invariant: the retry sent the ROTATED Bearer token.
        assertEquals("Bearer OLD_TOKEN", firstResourceAuth.get(),
            "first resource hit must use the original (stale) Bearer token");
        assertEquals("Bearer NEW_TOKEN", secondResourceAuth.get(),
            "retry must use the freshly-rotated Bearer token from AuthManager");

        // Sanity: exactly two resource hits — no more retries beyond the one-shot.
        assertEquals(2, resourceHits.get(),
            "exactly two resource hits: original 401 + one-shot retry 200");
    }
}
