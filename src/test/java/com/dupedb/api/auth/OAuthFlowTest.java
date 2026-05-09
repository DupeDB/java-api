package com.dupedb.api.auth;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OAuthFlow} focused on the parts that don't require a real
 * browser launch:
 * <ul>
 *   <li>The static {@code parseParam} query-string helper (preserved from Phase 99).</li>
 *   <li>The constructor wiring (default port = 0 / ephemeral; appId stored).</li>
 *   <li>The {@code exchangeCodeForToken} step against an in-process {@code HttpServer}
 *       — verifies the form-urlencoded wire shape against
 *       {@code /api/oauth/token} and that the JSON response maps to a
 *       5-field {@link Credentials} record.</li>
 * </ul>
 *
 * <p>AuthManager-related assertions (memory/disk token resolution, fresh-flow
 * fallback, single-flight refresh) move to {@code AuthManagerTest} in
 * plan 103-11. The browser-launch + loopback-callback handler is exercised
 * end-to-end only by the integration suite.</p>
 */
class OAuthFlowTest {

    // --- OAuthFlow.parseParam tests (PRESERVED from Phase 99) ---

    @Test
    void parseParamExtractsCodeFromQuery() {
        String token = OAuthFlow.parseParam("code=dupe_abc123&state=xyz", "code");
        assertEquals("dupe_abc123", token);
    }

    @Test
    void parseParamExtractsErrorFromQuery() {
        String error = OAuthFlow.parseParam("error=access_denied", "error");
        assertEquals("access_denied", error);
    }

    @Test
    void parseParamReturnsNullForMissingParam() {
        String result = OAuthFlow.parseParam("code=abc123", "error");
        assertNull(result);
    }

    @Test
    void parseParamReturnsNullForNullQuery() {
        String result = OAuthFlow.parseParam(null, "code");
        assertNull(result);
    }

    @Test
    void parseParamDecodesUrlEncodedValues() {
        String result = OAuthFlow.parseParam("code=dupe_abc%2B123", "code");
        assertEquals("dupe_abc+123", result);
    }

    @Test
    void parseParamHandlesEmptyValue() {
        String result = OAuthFlow.parseParam("code=&error=denied", "code");
        assertEquals("", result);
    }

    @Test
    void parseParamHandlesValueWithEquals() {
        // value contains = sign
        String result = OAuthFlow.parseParam("code=abc=def", "code");
        assertEquals("abc=def", result);
    }

    // --- OAuthFlow constructor tests ---

    @Test
    void defaultPortIsZero() {
        OAuthFlow flow = new OAuthFlow("https://dupedb.net", "my-app");
        // Constructor pins requestedCallbackPort = 0 (ephemeral). resolvedPort
        // is -1 until authenticateAndExchange() runs (we don't actually run
        // it here — the browser-launch is not test-friendly).
        assertEquals(-1, flow.getResolvedPort(),
            "resolvedPort must be -1 before authenticate; bind happens at runtime");
    }

    @Test
    void appIdIsStored() {
        OAuthFlow flow = new OAuthFlow("https://dupedb.net", "scanner-app");
        assertEquals("scanner-app", flow.getAppId());
    }

    @Test
    void appIdAccessorReturnsConstructorValue() {
        OAuthFlow flow = new OAuthFlow("https://dupedb.net", "scanner-bot");
        assertEquals("scanner-bot", flow.getAppId());
    }

    // --- /api/oauth/token wire-format test ---

    @Test
    void exchangeCodeBuildsFormUrlEncodedBodyAgainstLocalServer() throws Exception {
        // Spin up a tiny HttpServer that asserts the form fields and
        // returns a /token response. This avoids brittle Gson stubbing
        // and tests the wire format directly.
        //
        // Reuses the com.sun.net.httpserver.HttpServer pattern already in OAuthFlow.
        // Bind to ephemeral port; capture POST body; respond with canned JSON.

        com.sun.net.httpserver.HttpServer server =
            com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        final AtomicReference<String> capturedBody = new AtomicReference<>();
        final AtomicReference<String> capturedContentType = new AtomicReference<>();

        server.createContext("/api/oauth/token", exchange -> {
            capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            capturedBody.set(new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8));
            String json = "{\"access_token\":\"AT_FIXTURE\",\"token_type\":\"Bearer\","
                       + "\"expires_in\":3600,\"refresh_token\":\"RT_FIXTURE\"}";
            byte[] resBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resBytes.length);
            try (var os = exchange.getResponseBody()) { os.write(resBytes); }
        });
        server.start();

        try {
            OAuthFlow flow = new OAuthFlow("http://127.0.0.1:" + port, "test-app");
            Credentials c = flow.exchangeCodeForToken("CODE_FIXTURE", "VERIFIER_FIXTURE",
                "http://127.0.0.1:9876/callback");

            // Response → Credentials mapping
            assertEquals("AT_FIXTURE", c.accessToken());
            assertEquals("RT_FIXTURE", c.refreshToken());
            assertEquals("test-app", c.appId());
            assertNotNull(c.createdAt());
            assertNotNull(c.expiresAt());

            // Wire-format assertions on the captured POST body
            String body = capturedBody.get();
            assertNotNull(body);
            assertTrue(body.contains("grant_type=authorization_code"), body);
            assertTrue(body.contains("code=CODE_FIXTURE"), body);
            assertTrue(body.contains("code_verifier=VERIFIER_FIXTURE"), body);
            assertTrue(body.contains("client_id=test-app"), body);
            // redirect_uri is URL-encoded in the body — assert the encoded form
            assertTrue(body.contains("redirect_uri=http%3A%2F%2F127.0.0.1%3A9876%2Fcallback"), body);

            // Content-Type header must be form-urlencoded (RFC 6749 §4.1.3)
            assertEquals("application/x-www-form-urlencoded", capturedContentType.get());
        } finally {
            server.stop(0);
        }
    }
}
