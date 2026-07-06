package com.dupedb.api.internal;

import com.dupedb.api.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HttpExecutor's response mapping logic and lazy token resolution.
 * Uses the package-private mapResponse method to test status-to-exception mapping
 * without needing to mock java.net.http.HttpClient.
 */
class HttpExecutorTest {

    /** Simple record for deserialization tests. */
    record TestPayload(String name) {}

    private HttpExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new HttpExecutor("https://dupedb.net", null);
    }

    // --- Token supplier tests ---

    @Test
    void get_withNullTokenSupplier_doesNotAddAuthorizationHeader() {
        HttpExecutor exec = new HttpExecutor("https://dupedb.net", null);
        Map<String, String> headers = exec.buildHeaders(null);
        assertFalse(headers.containsKey("Authorization"));
        assertEquals("application/json", headers.get("Accept"));
    }

    @Test
    void get_withTokenSupplier_addsAuthorizationBearerHeader() {
        HttpExecutor exec = new HttpExecutor("https://dupedb.net", () -> "dupe_abc123");
        Map<String, String> headers = exec.buildHeaders(null);
        assertEquals("Bearer dupe_abc123", headers.get("Authorization"));
    }

    @Test
    void get_withTokenSupplierReturningNull_doesNotAddAuthorizationHeader() {
        HttpExecutor exec = new HttpExecutor("https://dupedb.net", () -> null);
        Map<String, String> headers = exec.buildHeaders(null);
        assertFalse(headers.containsKey("Authorization"));
    }

    @Test
    void tokenSupplier_calledPerRequest_notCachedAtConstructionTime() {
        AtomicReference<String> token = new AtomicReference<>("first_token");
        HttpExecutor exec = new HttpExecutor("https://dupedb.net", token::get);

        Map<String, String> headers1 = exec.buildHeaders(null);
        assertEquals("Bearer first_token", headers1.get("Authorization"));

        token.set("second_token");
        Map<String, String> headers2 = exec.buildHeaders(null);
        assertEquals("Bearer second_token", headers2.get("Authorization"));
    }

    @Test
    void post_headersIncludeContentType() {
        HttpExecutor exec = new HttpExecutor("https://dupedb.net", () -> "tok");
        Map<String, String> headers = exec.buildHeaders("application/json");
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("application/json", headers.get("Accept"));
        assertEquals("Bearer tok", headers.get("Authorization"));
    }

    // --- User-Agent ---

    @Test
    void buildHeaders_includesUserAgentWithSdkVersion() {
        Map<String, String> headers = executor.buildHeaders(null);
        assertEquals("dupedb-java/" + com.dupedb.api.DupeDB.VERSION, headers.get("User-Agent"));
    }

    @Test
    void requests_sendUserAgentHeaderOnTheWire() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(
            new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> receivedUserAgent = new AtomicReference<>();
        server.createContext("/api/ping", exchange -> {
            receivedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            byte[] body = "{\"name\":\"ok\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try (HttpExecutor exec = new HttpExecutor(
                "http://127.0.0.1:" + server.getAddress().getPort(), null)) {
            TestPayload result = exec.get("/api/ping", TestPayload.class);
            assertEquals("ok", result.name());
        } finally {
            server.stop(0);
        }
        assertEquals("dupedb-java/" + com.dupedb.api.DupeDB.VERSION, receivedUserAgent.get());
    }

    // --- Response mapping tests (status code -> exception) ---

    @Test
    void mapResponse_200_returnsBody() throws DupeDBException {
        TestPayload result = executor.mapResponse(200, "{\"name\":\"test\"}", emptyHeaders(), TestPayload.class);
        assertNotNull(result);
        assertEquals("test", result.name());
    }

    @Test
    void mapResponse_201_returnsBody() throws DupeDBException {
        TestPayload result = executor.mapResponse(201, "{\"name\":\"test\"}", emptyHeaders(), TestPayload.class);
        assertNotNull(result);
        assertEquals("test", result.name());
    }

    @Test
    void mapResponse_204_returnsNull() throws DupeDBException {
        Object result = executor.mapResponse(204, "", emptyHeaders(), Void.class);
        assertNull(result);
    }

    @Test
    void mapResponse_401_throwsAuthException() {
        AuthException ex = assertThrows(AuthException.class, () ->
            executor.mapResponse(401, "{\"error\":\"Invalid or expired token\"}", emptyHeaders(), String.class)
        );
        assertEquals("Invalid or expired token", ex.getMessage());
    }

    @Test
    void mapResponse_403_throwsAuthException() {
        AuthException ex = assertThrows(AuthException.class, () ->
            executor.mapResponse(403, "{\"error\":\"Forbidden\"}", emptyHeaders(), String.class)
        );
        assertEquals("Forbidden", ex.getMessage());
    }

    @Test
    void mapResponse_404_throwsApiExceptionWith404() {
        ApiException ex = assertThrows(ApiException.class, () ->
            executor.mapResponse(404, "{\"error\":\"Not found\"}", emptyHeaders(), String.class)
        );
        assertEquals(404, ex.getStatusCode());
        assertEquals("Not found", ex.getMessage());
    }

    @Test
    void mapResponse_429_throwsRateLimitException() {
        HttpHeaders headers = buildHeaders(Map.of("RateLimit-Reset", List.of("30")));
        RateLimitException ex = assertThrows(RateLimitException.class, () ->
            executor.mapResponse(429, "", headers, String.class)
        );
        assertEquals(30, ex.getRetryAfterSeconds());
    }

    @Test
    void mapResponse_429_withoutResetHeader_defaultsTo60() {
        RateLimitException ex = assertThrows(RateLimitException.class, () ->
            executor.mapResponse(429, "", emptyHeaders(), String.class)
        );
        assertEquals(60, ex.getRetryAfterSeconds());
    }

    @Test
    void mapResponse_500_throwsApiException() {
        ApiException ex = assertThrows(ApiException.class, () ->
            executor.mapResponse(500, "{\"error\":\"Internal server error\"}", emptyHeaders(), String.class)
        );
        assertEquals(500, ex.getStatusCode());
        assertEquals("Internal server error", ex.getMessage());
    }

    @Test
    void mapResponse_errorBodyFallsBackToRawBody() {
        ApiException ex = assertThrows(ApiException.class, () ->
            executor.mapResponse(500, "plain text error", emptyHeaders(), String.class)
        );
        assertEquals(500, ex.getStatusCode());
        assertEquals("plain text error", ex.getMessage());
    }

    @Test
    void mapResponse_401_errorBodyFallsBackToRawBody() {
        AuthException ex = assertThrows(AuthException.class, () ->
            executor.mapResponse(401, "Unauthorized", emptyHeaders(), String.class)
        );
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void mapResponse_errorFieldNull_fallsBackToRawBodyWithoutThrowing() {
        // {"error": null} previously triggered UnsupportedOperationException inside
        // the error parser, masking the real HTTP failure. It must now fall back to
        // the raw body and still surface the original status code.
        String body = "{\"error\":null}";
        ApiException ex = assertThrows(ApiException.class, () ->
            executor.mapResponse(500, body, emptyHeaders(), String.class)
        );
        assertEquals(500, ex.getStatusCode());
        assertEquals(body, ex.getMessage());
    }

    @Test
    void mapResponse_errorFieldObject_fallsBackToRawBody() {
        // A non-string "error" (object) must not break parsing either.
        String body = "{\"error\":{\"code\":123}}";
        ApiException ex = assertThrows(ApiException.class, () ->
            executor.mapResponse(404, body, emptyHeaders(), String.class)
        );
        assertEquals(body, ex.getMessage());
    }

    // --- IOException / InterruptedException wrapping tests ---

    @Test
    void wrapIOException_throwsNetworkException() {
        java.io.IOException ioEx = new java.io.IOException("Connection refused");
        NetworkException ex = assertThrows(NetworkException.class, () ->
            executor.wrapIOException(ioEx)
        );
        assertEquals("Connection failed", ex.getMessage());
        assertSame(ioEx, ex.getCause());
    }

    @Test
    void wrapInterruptedException_throwsNetworkExceptionAndResetsInterruptFlag() {
        InterruptedException intEx = new InterruptedException("interrupted");
        // Clear interrupt flag first
        Thread.interrupted();

        NetworkException ex = assertThrows(NetworkException.class, () ->
            executor.wrapInterruptedException(intEx)
        );
        assertEquals("Request interrupted", ex.getMessage());
        assertSame(intEx, ex.getCause());
        assertTrue(Thread.currentThread().isInterrupted(), "Interrupt flag should be re-set");

        // Clean up interrupt flag
        Thread.interrupted();
    }

    // --- Delete returns void ---

    @Test
    void mapResponse_deleteWith204_returnsNull() throws DupeDBException {
        Object result = executor.mapResponse(204, "", emptyHeaders(), Void.class);
        assertNull(result);
    }

    @Test
    void mapResponse_deleteWith200_returnsNull() throws DupeDBException {
        // Some APIs return 200 for delete with empty body
        Object result = executor.mapResponse(200, "", emptyHeaders(), Void.class);
        assertNull(result);
    }

    // --- encodeForm tests (form-urlencoded body builder) ---

    @Test
    void encodeForm_basicRoundTrip() {
        java.util.LinkedHashMap<String, String> form = new java.util.LinkedHashMap<>();
        form.put("token", "abc");
        form.put("token_type_hint", "refresh_token");
        assertEquals("token=abc&token_type_hint=refresh_token", HttpExecutor.encodeForm(form));
    }

    @Test
    void encodeForm_skipsNullValues() {
        java.util.LinkedHashMap<String, String> form = new java.util.LinkedHashMap<>();
        form.put("token", "abc");
        form.put("token_type_hint", null);
        assertEquals("token=abc", HttpExecutor.encodeForm(form));
    }

    @Test
    void encodeForm_emptyMapReturnsEmptyString() {
        assertEquals("", HttpExecutor.encodeForm(Map.of()));
    }

    @Test
    void encodeForm_escapesSpecialCharacters() {
        // URLEncoder spaces → '+', '=' → '%3D'
        java.util.LinkedHashMap<String, String> form = new java.util.LinkedHashMap<>();
        form.put("q", "hello world");
        form.put("eq", "a=b");
        assertEquals("q=hello+world&eq=a%3Db", HttpExecutor.encodeForm(form));
    }

    // --- Helper methods ---

    private static HttpHeaders emptyHeaders() {
        return buildHeaders(Map.of());
    }

    private static HttpHeaders buildHeaders(Map<String, List<String>> headerMap) {
        return HttpHeaders.of(headerMap, (name, value) -> true);
    }
}
