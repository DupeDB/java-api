package com.dupedb.api.auth;

import com.dupedb.api.exception.OAuthException;
import com.dupedb.api.internal.JsonHelper;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * OAuth 2.1 Authorization Code + PKCE flow.
 * <ol>
 *   <li>Generate a 32-byte base64url-no-pad code_verifier and S256 code_challenge.</li>
 *   <li>Bind a loopback HTTP server on an ephemeral port (RFC 8252 §7.3).</li>
 *   <li>Open the browser to {@code /api/oauth/authorize?response_type=code&client_id=&redirect_uri=&code_challenge=&code_challenge_method=S256&state=}.</li>
 *   <li>Capture {@code ?code=} from the loopback callback.</li>
 *   <li>POST to {@code /api/oauth/token} (form-urlencoded) with the code + verifier; receive a {@link Credentials} record.</li>
 * </ol>
 */
public class OAuthFlow {
    private static final int TIMEOUT_SECONDS = 300;

    private final String baseUrl;
    private final String appId;
    /** Caller-pinned port; {@code 0} (default) means the OS picks an ephemeral port at authenticate time. */
    private final int requestedCallbackPort;
    private volatile int resolvedPort = -1;

    public OAuthFlow(String baseUrl, String appId) {
        this(baseUrl, appId, 0);
    }

    public OAuthFlow(String baseUrl, String appId, int callbackPort) {
        this.baseUrl = baseUrl;
        this.appId = appId;
        this.requestedCallbackPort = callbackPort;
    }

    public String getAppId() { return appId; }

    /**
     * Package-private accessor — used by {@link AuthManager} for the
     * refresh-token POST URL construction (Phase 103 plan 103-11). The
     * field itself stays {@code private final}; this single-line accessor
     * is the only sanctioned out-of-class read of {@code baseUrl} and is
     * intentionally NOT public to keep the SDK's public surface minimal.
     */
    String baseUrl() { return baseUrl; }

    /**
     * Returns the actual callback port that was bound during the most
     * recent {@link #authenticateAndExchange()} call. Returns -1 if the
     * flow has not yet executed (or failed before bind).
     */
    public int getResolvedPort() { return resolvedPort; }

    /**
     * Run the complete OAuth 2.1 PKCE flow and return a Credentials record.
     *
     * <p>Blocking: opens the browser and blocks for up to {@value #TIMEOUT_SECONDS}s
     * for the user to complete consent. On timeout / error throws OAuthException.</p>
     */
    public Credentials authenticateAndExchange() throws OAuthException {
        if (!Desktop.isDesktopSupported()) {
            throw new OAuthException("Desktop browser not available — cannot complete OAuth flow");
        }

        String verifier = PkceUtil.generateCodeVerifier();
        String challenge = PkceUtil.deriveCodeChallenge(verifier);
        String state = newState();

        HttpServer server = null;
        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", requestedCallbackPort), 0);
            resolvedPort = server.getAddress().getPort();
            final HttpServer srv = server;

            final String redirectUri = "http://127.0.0.1:" + resolvedPort + "/callback";

            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String returnedCode = parseParam(query, "code");
                String returnedState = parseParam(query, "state");
                String error = parseParam(query, "error");

                String html;
                if (error != null) {
                    codeFuture.completeExceptionally(
                        new OAuthException("Authorization denied: " + error));
                    html = """
                        <html><body style="font-family:system-ui;text-align:center;padding:50px">
                        <h1>Authentication Failed</h1>
                        <p>You can close this tab and try again.</p>
                        </body></html>""";
                } else if (returnedCode == null || returnedState == null
                        || !returnedState.equals(state)) {
                    codeFuture.completeExceptionally(
                        new OAuthException("State mismatch — possible CSRF"));
                    html = """
                        <html><body style="font-family:system-ui;text-align:center;padding:50px">
                        <h1>Authentication Failed</h1>
                        <p>Security check failed (state mismatch). Close this tab and try again.</p>
                        </body></html>""";
                } else {
                    codeFuture.complete(returnedCode);
                    // PRESERVE 1.x HTML body verbatim per REQ-14 visual parity
                    html = """
                        <html><body style="font-family:system-ui;text-align:center;padding:50px">
                        <h1>Authenticated</h1>
                        <p>You can close this tab and return to your application.</p>
                        </body></html>""";
                }

                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                // Stop the server in a background thread — calling stop() inside the
                // handler deadlocks the listening loop.
                new Thread(() -> srv.stop(0)).start();
            });
            server.start();

            String authorizeUrl = baseUrl + "/api/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + URLEncoder.encode(appId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&code_challenge=" + URLEncoder.encode(challenge, StandardCharsets.UTF_8)
                + "&code_challenge_method=S256"
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

            Desktop.getDesktop().browse(URI.create(authorizeUrl));

            String code;
            try {
                code = codeFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                throw new OAuthException("OAuth flow timed out after " + TIMEOUT_SECONDS + "s");
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new OAuthException("OAuth flow interrupted", ie);
            } catch (Exception ee) {
                Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
                if (cause instanceof OAuthException oe) throw oe;
                throw new OAuthException("OAuth flow failed: " + cause.getMessage(), cause);
            }

            return exchangeCodeForToken(code, verifier, redirectUri);
        } catch (IOException ioe) {
            throw new OAuthException("Failed to bind loopback callback server", ioe);
        } finally {
            if (server != null) {
                try { server.stop(0); } catch (RuntimeException ignored) {}
            }
        }
    }

    /**
     * POST to /api/oauth/token (form-urlencoded) and parse the response
     * into a Credentials record.
     */
    Credentials exchangeCodeForToken(String code, String verifier, String redirectUri)
            throws OAuthException {
        String body = "grant_type=authorization_code"
            + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
            + "&client_id=" + URLEncoder.encode(appId, StandardCharsets.UTF_8)
            + "&code_verifier=" + URLEncoder.encode(verifier, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/oauth/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(30))
            .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new OAuthException("Token exchange failed: " + res.statusCode()
                    + " " + res.body());
            }
            TokenResponse tr = JsonHelper.fromJson(res.body(), TokenResponse.class);
            Instant expiresAt = Instant.now().plusSeconds(tr.expiresIn);
            return new Credentials(
                tr.accessToken,
                appId,
                Instant.now().toString(),
                tr.refreshToken,
                expiresAt
            );
        } catch (IOException ioe) {
            throw new OAuthException("Token exchange network error", ioe);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new OAuthException("Token exchange interrupted", ie);
        }
    }

    /** RFC 6749 §10.12 — fresh CSRF state per call. */
    private static String newState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Internal /token response shape. Field names use camelCase; Gson is
     * configured with {@code LOWER_CASE_WITH_UNDERSCORES} naming policy
     * (see {@link JsonHelper}) so they map to {@code access_token},
     * {@code expires_in}, etc. on the wire.
     */
    private static class TokenResponse {
        String accessToken;
        String tokenType;
        long expiresIn;
        String refreshToken;
        String scope;
    }

    /**
     * Parse a single query parameter value from a URL query string.
     * Package-private — used by the loopback callback handler AND by tests.
     */
    static String parseParam(String query, String name) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
