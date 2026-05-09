package com.dupedb.api.auth;

import com.dupedb.api.exception.AuthException;
import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 401 retry path tests for HttpExecutor (Phase 103 D-14).
 *
 * <p>The class under test lives in {@code com.dupedb.api.internal.HttpExecutor};
 * this test sits in the {@code auth} package because the BEHAVIOR being
 * verified is the auth-retry contract — see HttpExecutorAuthIntegrationTest
 * for the matching real-AuthManager + real-HttpExecutor end-to-end seam test.
 *
 * <p>This test uses a STUB AuthManager subclass that overrides
 * {@link AuthManager#forceRefresh()} and {@link AuthManager#getToken()} to
 * rotate an {@link AtomicReference}; the real refresh-lock and
 * postRefresh-network paths are exercised by AuthManagerTest (plan 103-11).
 */
class HttpExecutorRetryTest {

    private HttpServer server;
    private int port;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicReference<String> firstAuthHeader = new AtomicReference<>();
    private final AtomicReference<String> secondAuthHeader = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        requestCount.set(0);
        firstAuthHeader.set(null);
        secondAuthHeader.set(null);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/test", exchange -> {
            int n = requestCount.incrementAndGet();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (n == 1) {
                firstAuthHeader.set(auth);
                // Simulate stale token — 401 + WWW-Authenticate invalid_token
                exchange.getResponseHeaders().add("WWW-Authenticate",
                    "Bearer realm=\"api\", error=\"invalid_token\"");
                byte[] body = "{\"error\":\"invalid_token\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(401, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
            } else {
                secondAuthHeader.set(auth);
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
    void oneShot401TriggersRefreshAndReplay() throws Exception {
        // AtomicReference simulates the AuthManager — first .get() returns
        // OLD_TOKEN, after forceRefresh() returns NEW_TOKEN.
        AtomicReference<String> currentToken = new AtomicReference<>("OLD_TOKEN");
        // Stub AuthManager whose forceRefresh just rotates the supplier source.
        // AuthManager(TokenStore, OAuthFlow) tolerates null OAuthFlow; the
        // overridden methods never reach the disabled refresh-network path.
        AuthManager stubManager = new AuthManager(
            new TokenStore(Path.of(System.getProperty("java.io.tmpdir"), "stub.json")),
            null
        ) {
            @Override
            public void forceRefresh() {
                currentToken.set("NEW_TOKEN");
            }
            @Override
            public String getToken() {
                return currentToken.get();
            }
        };

        HttpExecutor exec = new HttpExecutor(
            "http://127.0.0.1:" + port,
            currentToken::get,
            stubManager
        );

        // Make a GET — should: (1) hit /api/test with Bearer OLD_TOKEN -> 401,
        // (2) call stubManager.forceRefresh() (rotates token), (3) retry with Bearer NEW_TOKEN -> 200.
        java.util.Map<?, ?> result = exec.get("/api/test", java.util.Map.class);
        assertNotNull(result);
        assertEquals(true, result.get("ok"));

        assertEquals(2, requestCount.get(), "exactly two requests: original 401 + retry 200");
        assertEquals("Bearer OLD_TOKEN", firstAuthHeader.get(), "first request used the old token");
        assertEquals("Bearer NEW_TOKEN", secondAuthHeader.get(), "retry used the refreshed token");
    }

    @Test
    void secondConsecutive401Throws() throws Exception {
        // Reconfigure the server to ALWAYS return 401.
        server.removeContext("/api/test");
        server.createContext("/api/test", exchange -> {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().add("WWW-Authenticate",
                "Bearer realm=\"api\", error=\"invalid_token\"");
            byte[] body = "{\"error\":\"invalid_token\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        AtomicReference<String> currentToken = new AtomicReference<>("ANY_TOKEN");
        AuthManager stubManager = new AuthManager(
            new TokenStore(Path.of(System.getProperty("java.io.tmpdir"), "stub2.json")),
            null
        ) {
            @Override public void forceRefresh() { currentToken.set("ANOTHER_TOKEN"); }
            @Override public String getToken() { return currentToken.get(); }
        };

        HttpExecutor exec = new HttpExecutor(
            "http://127.0.0.1:" + port,
            currentToken::get,
            stubManager
        );

        DupeDBException thrown = assertThrows(DupeDBException.class,
            () -> exec.get("/api/test", java.util.Map.class));
        assertTrue(thrown instanceof AuthException,
            "second consecutive 401 must throw AuthException, got: " + thrown);
        assertEquals(2, requestCount.get(), "exactly 2 requests -- no infinite spin");
    }

    @Test
    void non401PassesThrough() throws Exception {
        server.removeContext("/api/test");
        server.createContext("/api/test", exchange -> {
            requestCount.incrementAndGet();
            String json = "{\"hello\":\"world\"}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        HttpExecutor exec = new HttpExecutor(
            "http://127.0.0.1:" + port,
            () -> "WHATEVER_TOKEN",
            null                                // no AuthManager — retry disabled
        );

        java.util.Map<?, ?> result = exec.get("/api/test", java.util.Map.class);
        assertEquals("world", result.get("hello"));
        assertEquals(1, requestCount.get(), "no retry on 200");
    }
}
