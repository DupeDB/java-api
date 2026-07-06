package com.dupedb.api.internal;

import com.dupedb.api.exception.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Internal HTTP client wrapper. Handles auth headers, JSON (de)serialization, and error mapping. */
public class HttpExecutor implements AutoCloseable {
    /** Sent on every request so SDK traffic is identifiable server-side. */
    static final String USER_AGENT = "dupedb-java/" + com.dupedb.api.DupeDB.VERSION;

    private final String baseUrl;
    private final Supplier<String> tokenSupplier;
    private final HttpClient httpClient;

    /**
     * Optional — when set (via the 3-arg constructor), enables one-shot 401
     * retry per Phase 103 D-14. Non-final by design: the 2-arg constructor
     * leaves this null (legacy / direct-token mode); the 3-arg constructor
     * assigns it to opt into the refresh-and-retry path.
     */
    private com.dupedb.api.auth.AuthManager authManagerRef = null;

    /**
     * Creates a new HttpExecutor.
     *
     * @param baseUrl       the API base URL (e.g. "https://dupedb.net")
     * @param tokenSupplier nullable; resolves the current auth token per-request
     */
    public HttpExecutor(String baseUrl, Supplier<String> tokenSupplier) {
        this.baseUrl = baseUrl;
        this.tokenSupplier = tokenSupplier;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Creates a new HttpExecutor with an {@link com.dupedb.api.auth.AuthManager}
     * wired in for the Phase 103 D-14 one-shot 401 retry path.
     *
     * <p>On a {@code 401} response with {@code WWW-Authenticate: Bearer ... invalid_token}
     * (or a {@code 401} with no {@code WWW-Authenticate} header — lenient),
     * this executor calls {@link com.dupedb.api.auth.AuthManager#forceRefresh()}
     * exactly once, rebuilds the request with the rotated token from
     * {@code tokenSupplier}, and retries. A second consecutive {@code 401}
     * surfaces as {@link com.dupedb.api.exception.AuthException} (no silent spin).</p>
     *
     * @param baseUrl       the API base URL (e.g. "https://dupedb.net")
     * @param tokenSupplier nullable; resolves the current auth token per-request
     * @param authManager   nullable; when non-null, enables the 401 retry path
     */
    public HttpExecutor(String baseUrl, java.util.function.Supplier<String> tokenSupplier,
                        com.dupedb.api.auth.AuthManager authManager) {
        this(baseUrl, tokenSupplier);
        this.authManagerRef = authManager;
    }

    /** Closes the underlying HTTP client, releasing any pooled connections. */
    @Override
    public void close() {
        httpClient.close();
    }

    /**
     * Sends a GET request and deserializes the response body.
     *
     * @param path the API path (appended to baseUrl)
     * @param type the class to deserialize into
     * @param <T>  the response type
     * @return the deserialized response
     * @throws DupeDBException if the request fails
     */
    public <T> T get(String path, Class<T> type) throws DupeDBException {
        HttpRequest request = buildRequest(path)
            .GET()
            .build();
        return execute(request, type);
    }

    /**
     * Sends a GET request and deserializes the response into a generic type.
     * Use with {@code TypeToken} for parameterized types like {@code SearchResult<Exploit>}.
     *
     * @param path the API path (appended to baseUrl)
     * @param type the generic type to deserialize into
     * @param <T>  the response type
     * @return the deserialized response
     * @throws DupeDBException if the request fails
     */
    public <T> T get(String path, Type type) throws DupeDBException {
        HttpRequest request = buildRequest(path)
            .GET()
            .build();
        return execute(request, type);
    }

    /**
     * Sends a POST request with a JSON body and deserializes the response.
     *
     * @param path the API path (appended to baseUrl)
     * @param body the request body to serialize as JSON
     * @param type the class to deserialize the response into
     * @param <T>  the response type
     * @return the deserialized response
     * @throws DupeDBException if the request fails
     */
    public <T> T post(String path, Object body, Class<T> type) throws DupeDBException {
        String json = JsonHelper.toJson(body);
        HttpRequest request = buildRequest(path)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        return execute(request, type);
    }

    /**
     * Sends a PUT request with a JSON body and deserializes the response.
     *
     * @param path the API path (appended to baseUrl)
     * @param body the request body to serialize as JSON
     * @param type the class to deserialize the response into
     * @param <T>  the response type
     * @return the deserialized response
     * @throws DupeDBException if the request fails
     */
    public <T> T put(String path, Object body, Class<T> type) throws DupeDBException {
        String json = JsonHelper.toJson(body);
        HttpRequest request = buildRequest(path)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();
        return execute(request, type);
    }

    /**
     * Sends a PATCH request with a JSON body and deserializes the response.
     * {@link HttpRequest.Builder} has no {@code .PATCH()} shortcut so this uses
     * {@code .method("PATCH", ...)} explicitly.
     *
     * @param path the API path (appended to baseUrl)
     * @param body the request body to serialize as JSON
     * @param type the class to deserialize the response into
     * @param <T>  the response type
     * @return the deserialized response
     * @throws DupeDBException if the request fails
     */
    public <T> T patch(String path, Object body, Class<T> type) throws DupeDBException {
        String json = JsonHelper.toJson(body);
        HttpRequest request = buildRequest(path)
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
            .build();
        return execute(request, type);
    }

    /**
     * Sends a POST with {@code application/x-www-form-urlencoded} body. Used by
     * RFC 7009 {@code /api/oauth/revoke} and other form endpoints.
     *
     * <p>Keys/values are URL-encoded with UTF-8. Entries whose value is {@code null}
     * are skipped (so callers can pass an optional hint without a guard).</p>
     *
     * @param path     the API path (appended to baseUrl)
     * @param formData map of form field name to value
     * @param type     the class to deserialize the response into
     * @param <T>      the response type
     * @return the deserialized response
     * @throws DupeDBException if the request fails
     */
    public <T> T postForm(String path, Map<String, String> formData, Class<T> type)
            throws DupeDBException {
        String body = encodeForm(formData);
        HttpRequest request = buildRequest(path)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        return execute(request, type);
    }

    /**
     * Sends a DELETE request. No response body is expected.
     *
     * @param path the API path (appended to baseUrl)
     * @throws DupeDBException if the request fails
     */
    public void delete(String path) throws DupeDBException {
        HttpRequest request = buildRequest(path)
            .DELETE()
            .build();
        execute(request, Void.class);
    }

    /**
     * Sends a DELETE request and deserializes the response body.
     * Use for DELETE endpoints that return JSON (e.g. confirmation with freed size).
     *
     * @param path the API path (appended to baseUrl)
     * @param type the class to deserialize the response into
     * @param <T>  the response type
     * @return the deserialized response
     * @throws DupeDBException if the request fails
     */
    public <T> T deleteWithResponse(String path, Class<T> type) throws DupeDBException {
        HttpRequest request = buildRequest(path)
            .DELETE()
            .build();
        return execute(request, type);
    }

    /**
     * Sends a POST request with a multipart/form-data body and deserializes the response.
     * Use for file upload endpoints.
     *
     * <p>The parts map accepts two value types:
     * <ul>
     *   <li>{@link Path} — sent as a file part with content type probed from the file</li>
     *   <li>{@link String} — sent as a text field</li>
     * </ul>
     *
     * @param path  the API path (appended to baseUrl)
     * @param parts map of field name to value (Path for files, String for text)
     * @param type  the class to deserialize the response into
     * @param <T>   the response type
     * @return the deserialized response
     * @throws DupeDBException if the request fails
     */
    public <T> T postMultipart(String path, Map<String, Object> parts, Class<T> type)
            throws DupeDBException {
        String boundary = "DupeDB-" + UUID.randomUUID();
        HttpRequest request = buildRequest(path)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(buildMultipartBody(boundary, parts))
            .build();
        return execute(request, type);
    }

    // --- Package-private methods for testing ---

    /**
     * Builds the request headers map. Exposed for testing token resolution behavior.
     *
     * @param contentType nullable content type (set for POST/PUT)
     * @return map of header name to value
     */
    Map<String, String> buildHeaders(String contentType) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json");
        headers.put("User-Agent", USER_AGENT);

        if (tokenSupplier != null) {
            String token = tokenSupplier.get();
            if (token != null) {
                headers.put("Authorization", "Bearer " + token);
            }
        }

        if (contentType != null) {
            headers.put("Content-Type", contentType);
        }

        return headers;
    }

    /**
     * Maps an HTTP response to either a deserialized result or an exception.
     * Exposed as package-private for direct testing of the mapping logic.
     *
     * @param statusCode the HTTP status code
     * @param body       the response body string
     * @param headers    the response headers
     * @param type       the class to deserialize success responses into
     * @param <T>        the response type
     * @return the deserialized response, or null for 204/Void
     * @throws DupeDBException the appropriate exception for error status codes
     */
    <T> T mapResponse(int statusCode, String body, HttpHeaders headers, Class<T> type)
            throws DupeDBException {
        return switch (statusCode) {
            case 200, 201 -> {
                if (type == Void.class || body == null || body.isEmpty()) {
                    yield null;
                }
                yield JsonHelper.fromJson(body, type);
            }
            case 204 -> null;
            case 401 -> throw new AuthException(parseErrorMessage(body));
            case 403 -> throw new AuthException(parseErrorMessage(body));
            case 429 -> throw new RateLimitException(parseRetryAfter(headers));
            default -> {
                if (statusCode >= 400) {
                    throw new ApiException(statusCode, parseErrorMessage(body));
                }
                // For any other 2xx codes, try to deserialize
                if (type == Void.class || body == null || body.isEmpty()) {
                    yield null;
                }
                yield JsonHelper.fromJson(body, type);
            }
        };
    }

    /**
     * URL-encodes a form data map as {@code application/x-www-form-urlencoded}.
     * Entries whose value is {@code null} are skipped so callers may pass
     * optional fields unconditionally. Package-private for direct testing.
     */
    static String encodeForm(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (e.getValue() == null) continue;
            if (!first) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Wraps an IOException as a NetworkException. Exposed for testing.
     *
     * @param e the IOException to wrap
     * @throws NetworkException always thrown
     */
    void wrapIOException(IOException e) throws NetworkException {
        throw new NetworkException("Connection failed", e);
    }

    /**
     * Wraps an InterruptedException as a NetworkException and re-sets the interrupt flag.
     * Exposed for testing.
     *
     * @param e the InterruptedException to wrap
     * @throws NetworkException always thrown
     */
    void wrapInterruptedException(InterruptedException e) throws NetworkException {
        Thread.currentThread().interrupt();
        throw new NetworkException("Request interrupted", e);
    }

    // --- Private implementation ---

    private HttpRequest.BodyPublisher buildMultipartBody(String boundary, Map<String, Object> parts)
            throws DupeDBException {
        List<byte[]> byteArrays = new ArrayList<>();
        byte[] separator = ("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8);

        try {
            for (Map.Entry<String, Object> entry : parts.entrySet()) {
                byteArrays.add(separator);
                String name = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Path filePath) {
                    String filename = filePath.getFileName().toString();
                    String contentType = Files.probeContentType(filePath);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                    byteArrays.add(("Content-Disposition: form-data; name=\"" + escapeHeaderField(name)
                        + "\"; filename=\"" + escapeHeaderField(filename) + "\"\r\n"
                        + "Content-Type: " + contentType + "\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                    byteArrays.add(Files.readAllBytes(filePath));
                } else {
                    byteArrays.add(("Content-Disposition: form-data; name=\"" + escapeHeaderField(name) + "\"\r\n\r\n"
                        + value).getBytes(StandardCharsets.UTF_8));
                }
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new NetworkException("Failed to read file for upload", e);
        }

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    /**
     * Escapes a multipart header parameter (a form field {@code name} or a
     * {@code filename}) so a stray quote or line break cannot break out of the
     * {@code Content-Disposition} header. Percent-encodes CR, LF, and {@code "}
     * per the WHATWG/RFC 2388 convention.
     */
    private static String escapeHeaderField(String value) {
        return value
            .replace("\r", "%0D")
            .replace("\n", "%0A")
            .replace("\"", "%22");
    }

    private HttpRequest.Builder buildRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(30));

        // Single source of truth for common headers (also the test seam), so the
        // two can't drift apart.
        buildHeaders(null).forEach(builder::header);

        return builder;
    }

    private <T> T execute(HttpRequest request, Class<T> type) throws DupeDBException {
        return executeWithRetry(request, type, false);
    }

    private <T> T execute(HttpRequest request, Type type) throws DupeDBException {
        return executeWithRetry(request, type, false);
    }

    /**
     * Single send-and-map with the Phase 103 D-14 one-shot 401 retry layered
     * on top. When {@link #authManagerRef} is non-null and the response is a
     * {@code 401} matching the retry guard (see
     * {@link #shouldRetryAfterRefresh(HttpHeaders)}), this method calls
     * {@link com.dupedb.api.auth.AuthManager#forceRefresh()} and recurses with
     * {@code retried=true}; on the second {@code 401} the {@link AuthException}
     * from {@link #mapResponse(int, String, HttpHeaders, Class)} surfaces.
     */
    private <T> T executeWithRetry(HttpRequest request, Class<T> type, boolean retried)
            throws DupeDBException {
        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            int status = response.statusCode();
            HttpHeaders responseHeaders = response.headers();

            if (status == 401 && !retried && authManagerRef != null
                    && shouldRetryAfterRefresh(responseHeaders)) {
                authManagerRef.forceRefresh();              // D-14
                HttpRequest retryReq = rebuildWithFreshToken(request);
                return executeWithRetry(retryReq, type, true);
            }
            return mapResponse(status, response.body(), responseHeaders, type);
        } catch (IOException e) {
            throw new NetworkException("Connection failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkException("Request interrupted", e);
        }
    }

    /**
     * {@link Type}-overload of {@link #executeWithRetry(HttpRequest, Class, boolean)}.
     * Mirrors the original {@code execute(HttpRequest, Type)} mapping logic
     * verbatim and wraps it in the same one-shot retry envelope.
     */
    private <T> T executeWithRetry(HttpRequest request, Type type, boolean retried)
            throws DupeDBException {
        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            int statusCode = response.statusCode();
            String body = response.body();
            HttpHeaders headers = response.headers();

            if (statusCode == 401 && !retried && authManagerRef != null
                    && shouldRetryAfterRefresh(headers)) {
                authManagerRef.forceRefresh();              // D-14
                HttpRequest retryReq = rebuildWithFreshToken(request);
                return executeWithRetry(retryReq, type, true);
            }

            return switch (statusCode) {
                case 200, 201 -> {
                    if (body == null || body.isEmpty()) {
                        yield null;
                    }
                    yield JsonHelper.fromJson(body, type);
                }
                case 204 -> null;
                case 401 -> throw new AuthException(parseErrorMessage(body));
                case 403 -> throw new AuthException(parseErrorMessage(body));
                case 429 -> throw new RateLimitException(parseRetryAfter(headers));
                default -> {
                    if (statusCode >= 400) {
                        throw new ApiException(statusCode, parseErrorMessage(body));
                    }
                    if (body == null || body.isEmpty()) {
                        yield null;
                    }
                    yield JsonHelper.fromJson(body, type);
                }
            };
        } catch (IOException e) {
            throw new NetworkException("Connection failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkException("Request interrupted", e);
        }
    }

    /**
     * Decide whether a {@code 401} response is the kind that warrants a
     * one-shot refresh-and-retry per Phase 103 D-14. Retries on
     * {@code WWW-Authenticate: Bearer ... invalid_token} (RFC 6750 §3) OR on
     * a bare {@code 401} with no {@code WWW-Authenticate} header (lenient —
     * some servers omit the header even though the spec says they should set it).
     * {@code 403} and {@code 5xx} are intentionally NOT retried.
     */
    private boolean shouldRetryAfterRefresh(HttpHeaders headers) {
        String wwwAuth = headers.firstValue("www-authenticate").orElse("");
        return wwwAuth.contains("invalid_token") || wwwAuth.isEmpty();
    }

    /**
     * Rebuild an {@link HttpRequest} with the freshly-refreshed Bearer token
     * in the {@code Authorization} header. The original request's URI, method,
     * timeout, body, and all non-{@code Authorization} headers are preserved
     * verbatim. Called from the D-14 retry path after
     * {@link com.dupedb.api.auth.AuthManager#forceRefresh()} rotates the token
     * inside the {@code tokenSupplier}.
     */
    private HttpRequest rebuildWithFreshToken(HttpRequest original) {
        HttpRequest.Builder b = HttpRequest.newBuilder(original.uri())
            .timeout(original.timeout().orElse(Duration.ofSeconds(30)))
            .method(
                original.method(),
                original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody())
            );
        // Copy non-Authorization headers verbatim.
        original.headers().map().forEach((k, vs) -> {
            if (!"Authorization".equalsIgnoreCase(k)) {
                for (String v : vs) {
                    b.header(k, v);
                }
            }
        });
        // Set fresh Authorization from the (now-rotated) supplier.
        if (tokenSupplier != null) {
            String tok = tokenSupplier.get();
            if (tok != null) {
                b.header("Authorization", "Bearer " + tok);
            }
        }
        return b.build();
    }

    /**
     * Parses an error message from a JSON response body.
     * The DupeDB server returns errors as {@code {"error": "message"}}.
     * Falls back to the raw body if JSON parsing fails.
     */
    private String parseErrorMessage(String body) {
        if (body == null || body.isEmpty()) {
            return "Unknown error";
        }
        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            // Only unwrap when "error" is a string. Guards against {"error": null}
            // (JsonNull.getAsString() throws) and {"error": {...}} — fall back to
            // the raw body in those cases rather than masking the real response.
            if (obj.has("error") && obj.get("error").isJsonPrimitive()) {
                return obj.get("error").getAsString();
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            // Not valid JSON or not an object -- fall through to raw body
        }
        return body;
    }

    /**
     * Parses the RateLimit-Reset header to determine retry-after seconds.
     * Defaults to 60 seconds if the header is missing or unparseable.
     */
    private int parseRetryAfter(HttpHeaders headers) {
        return headers.firstValue("RateLimit-Reset")
            .map(value -> {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return 60;
                }
            })
            .orElse(60);
    }
}
