package com.dupedb.api.auth;

import com.dupedb.api.exception.AuthException;
import com.dupedb.api.exception.DupeDBException;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AuthManager} covering the four mandates of plan 103-11:
 *
 * <ol>
 *   <li>Direct-token mode — preserved Phase-99 {@code AuthManager(String)} ctor.</li>
 *   <li>Disk-load — TokenStore.load() flow when no in-memory token exists.</li>
 *   <li>Single-flight refresh under concurrency (D-13) — N threads, exactly 1 /token POST.</li>
 *   <li>Refresh-failure fallback (D-15) — 400 invalid_grant clears TokenStore and runs
 *       a fresh OAuthFlow.authenticateAndExchange().</li>
 * </ol>
 *
 * <p>The single-flight and fallback tests stand up a real in-process
 * {@link HttpServer} on an ephemeral port to exercise the wire format end-to-end
 * (mirroring the {@link OAuthFlowTest#exchangeCodeBuildsFormUrlEncodedBodyAgainstLocalServer}
 * pattern from plan 103-10). The fallback test additionally subclasses
 * {@link OAuthFlow} to stub out {@code authenticateAndExchange()} since browser
 * launch is not test-friendly.</p>
 */
class AuthManagerTest {

    // ── Direct-token mode (Phase 99 ctor preserved) ──────────────────────────

    @Test
    void directTokenReturnsImmediately() throws DupeDBException {
        AuthManager manager = new AuthManager("direct_token_xyz");
        assertEquals("direct_token_xyz", manager.getToken());
        assertTrue(manager.hasToken());
    }

    // ── Disk-load path (no OAuthFlow needed when disk creds present) ─────────

    @Test
    void getTokenLoadsFromDiskWhenNoMemoryToken(@TempDir Path tempDir) throws Exception {
        Path tokenFile = tempDir.resolve("token.json");
        TokenStore store = new TokenStore(tokenFile);
        // 5-arg ctor — disk format must include refreshToken + expiresAt for
        // TokenStore.load() to NOT invalidate (Phase 103-09 D-16 gate)
        store.save(new Credentials(
            "disk_access_token",
            "my-app",
            "2026-04-06T12:00:00Z",
            "disk_refresh",
            Instant.now().plusSeconds(3600)
        ));
        AuthManager manager = new AuthManager(store, null);
        assertEquals("disk_access_token", manager.getToken());
    }

    // ── No-config edge: empty disk + null OAuthFlow ──────────────────────────

    @Test
    void noConfig_throwsAuthException(@TempDir Path tempDir) {
        // Empty TokenStore + null OAuthFlow — no path to acquire creds.
        TokenStore store = new TokenStore(tempDir.resolve("missing.json"));
        AuthManager manager = new AuthManager(store, null);
        DupeDBException thrown = assertThrows(DupeDBException.class, manager::getToken);
        assertTrue(thrown instanceof AuthException,
            "no-config path must throw AuthException specifically, got " + thrown.getClass());
    }

    // ── D-13 Single-flight refresh under concurrency ─────────────────────────

    @Test
    void singleFlightOnlyOneRefreshUnderConcurrency(@TempDir Path tempDir) throws Exception {
        // Stand up a fake /oauth/token server that counts hits and returns a
        // valid /token response. Prime AuthManager with already-expired
        // credentials on disk so that the first getToken() call from each
        // thread fires the proactive-refresh path.
        // D-13 invariant: exactly 1 server hit observed under N concurrent
        // callers (synchronized(refreshLock) + recheck-under-lock).
        final AtomicInteger callCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();

        server.createContext("/api/oauth/token", exchange -> {
            callCount.incrementAndGet();
            String json = "{\"access_token\":\"NEW_AT\",\"token_type\":\"Bearer\","
                + "\"expires_in\":3600,\"refresh_token\":\"NEW_RT\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + port;
            OAuthFlow flow = new OAuthFlow(baseUrl, "test-app");

            Path tokenFile = tempDir.resolve("token.json");
            TokenStore store = new TokenStore(tokenFile);
            store.save(new Credentials(
                "OLD_AT",
                "test-app",
                Instant.now().toString(),
                "OLD_RT",
                Instant.now().minusSeconds(10)        // ALREADY expired -> proactive refresh fires
            ));

            AuthManager mgr = new AuthManager(store, flow);

            final int N = 8;
            ExecutorService es = Executors.newFixedThreadPool(N);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(N);
            AtomicInteger errors = new AtomicInteger(0);
            for (int i = 0; i < N; i++) {
                es.submit(() -> {
                    try {
                        start.await();
                        String tok = mgr.getToken();
                        if (!"NEW_AT".equals(tok)) {
                            errors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(15, TimeUnit.SECONDS), "all " + N + " threads must complete within 15s");
            es.shutdownNow();

            assertEquals(0, errors.get(),
                "all " + N + " threads must receive the rotated NEW_AT token without exception");
            assertEquals(1, callCount.get(),
                "Single-flight invariant: exactly one /oauth/token POST under " + N + " concurrent callers");
        } finally {
            server.stop(0);
        }
    }

    // ── D-15 Refresh-failure → fresh OAuth flow ──────────────────────────────

    @Test
    void refreshFailureWithInvalidGrantClearsTokenStoreAndRunsFreshOAuth(@TempDir Path tempDir)
            throws Exception {
        // Fake server returns 400 invalid_grant on /token. AuthManager should:
        //   1. Detect "invalid_grant" in the OAuthException message internally.
        //   2. Delete the TokenStore file (D-15).
        //   3. Fall back to OAuthFlow.authenticateAndExchange() and persist the
        //      returned credentials.
        //
        // We can't actually invoke a browser in tests, so we subclass OAuthFlow
        // and override authenticateAndExchange() to return a canned Credentials.
        final AtomicInteger refreshHits = new AtomicInteger(0);
        final AtomicInteger fallbackHits = new AtomicInteger(0);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/api/oauth/token", exchange -> {
            refreshHits.incrementAndGet();
            String json = "{\"error\":\"invalid_grant\",\"error_description\":\"refresh token revoked\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + port;
            OAuthFlow flow = new OAuthFlow(baseUrl, "test-app") {
                @Override
                public Credentials authenticateAndExchange() {
                    fallbackHits.incrementAndGet();
                    return new Credentials("FRESH_AT", "test-app",
                        Instant.now().toString(), "FRESH_RT",
                        Instant.now().plusSeconds(3600));
                }
            };

            Path tokenFile = tempDir.resolve("token.json");
            TokenStore store = new TokenStore(tokenFile);
            store.save(new Credentials("OLD_AT", "test-app",
                Instant.now().toString(), "OLD_RT",
                Instant.now().minusSeconds(10)));

            AuthManager mgr = new AuthManager(store, flow);
            String token = mgr.getToken();      // proactive refresh fires -> 400 -> fallback fires
            assertEquals("FRESH_AT", token,
                "Fallback OAuthFlow.authenticateAndExchange must run on invalid_grant");
            assertEquals(1, refreshHits.get(), "Refresh attempted exactly once");
            assertEquals(1, fallbackHits.get(), "Fallback ran exactly once");

            // Disk side-effect: TokenStore now holds the fresh creds (D-15
            // re-persists after the fallback flow), not the deleted OLD_RT row.
            Credentials persisted = store.load();
            assertNotNull(persisted, "fallback path must re-persist fresh credentials to disk");
            assertEquals("FRESH_AT", persisted.accessToken());
            assertEquals("FRESH_RT", persisted.refreshToken());
        } finally {
            server.stop(0);
        }
    }
}
