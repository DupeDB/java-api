package com.dupedb.api.auth;

import com.dupedb.api.exception.AuthException;
import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.exception.OAuthException;
import com.dupedb.api.internal.JsonHelper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Resolves a usable Bearer access token for the SDK's HTTP layer.
 *
 * <p>Token acquisition chain:
 * <ol>
 *   <li>In-memory {@code current} credentials (volatile — visible across threads).</li>
 *   <li>{@link TokenStore} on-disk credentials (validated for the 5-field shape; 1.x files invalidate).</li>
 *   <li>{@link OAuthFlow#authenticateAndExchange()} — fresh user-driven OAuth.</li>
 * </ol>
 *
 * <p>Refresh strategy (Phase 103 D-13/D-14/D-15):
 * <ul>
 *   <li>Proactive: when remaining lifetime is within 60s of expiry, refresh before returning.</li>
 *   <li>Reactive: HttpExecutor (or any caller) on 401 calls {@link #forceRefresh()}.</li>
 *   <li>Single-flight: {@code synchronized (refreshLock)} + double-check ensures only one
 *       {@code /oauth/token} POST under N concurrent callers.</li>
 *   <li>Fallback: refresh failure with {@code invalid_grant} clears the TokenStore and
 *       delegates to {@code oAuthFlow.authenticateAndExchange()}.</li>
 * </ul>
 *
 * <p>{@code Supplier<String>} contract preserved from Phase 99 — {@code manager::getToken}
 * remains a valid token supplier for HttpExecutor.
 */
public class AuthManager {

    private static final long REFRESH_WINDOW_SECONDS = 60;

    private final TokenStore tokenStore;
    private final OAuthFlow oAuthFlow;
    private final String baseUrl;
    /**
     * Non-null when constructed via {@link #AuthManager(String)}; nulled out
     * by {@link #clearToken()} so direct-token mode is also clearable
     * (otherwise {@link #hasToken()} would always return {@code true} until
     * the AuthManager instance is discarded).
     */
    private volatile String directToken;
    private final Object refreshLock = new Object();

    /** Volatile so concurrent readers see the latest rotation without locking. */
    private volatile Credentials current;

    // ── Constructors ─────────────────────────────────────────────────

    /**
     * Direct-token mode. No refresh, no disk persistence. Used for tests
     * and CI scenarios where a long-lived service token is provided.
     */
    public AuthManager(String token) {
        this.tokenStore = null;
        this.oAuthFlow = null;
        this.baseUrl = null;
        this.directToken = token;
        this.current = null;
    }

    /**
     * Full mode: refresh-capable, disk-persisted. Pulls baseUrl from the
     * OAuthFlow so refresh POSTs hit the same host as the initial flow.
     */
    public AuthManager(TokenStore tokenStore, OAuthFlow oAuthFlow) {
        this.tokenStore = tokenStore;
        this.oAuthFlow = oAuthFlow;
        this.baseUrl = oAuthFlow != null ? oAuthFlow.baseUrl() : null;
        this.directToken = null;
        this.current = null;
    }

    // ── Public API ───────────────────────────────────────────────────

    /**
     * Resolve a valid access token. Blocks briefly during refresh.
     * Thread-safe — this is the {@code Supplier<String>} entry-point.
     */
    public String getToken() throws DupeDBException {
        if (directToken != null) return directToken;

        Credentials c = current;
        if (c == null) c = loadOrAuth();              // hydrate from disk or run fresh OAuth flow

        // Proactive refresh: if within REFRESH_WINDOW_SECONDS of expiry, rotate now
        if (c.expiresAt() != null
            && c.expiresAt().minusSeconds(REFRESH_WINDOW_SECONDS).isBefore(Instant.now())) {
            return refreshIfNeeded(c).accessToken();
        }
        return c.accessToken();
    }

    /**
     * Force a refresh immediately, regardless of expiry timing. Called by
     * HttpExecutor on 401 retry per D-14.
     */
    public void forceRefresh() throws DupeDBException {
        if (directToken != null) {
            // Direct-token mode has no refresh path; nothing to do.
            return;
        }
        Credentials c = current;
        if (c == null) {
            c = loadOrAuth();
        }
        refreshIfNeeded(c, /*force=*/ true);
    }

    public synchronized void clearToken() {
        current = null;
        directToken = null;
        if (tokenStore != null) {
            try { tokenStore.delete(); } catch (Exception ignored) { /* idempotent */ }
        }
    }

    public synchronized boolean hasToken() {
        if (directToken != null) return true;
        if (current != null) return true;
        if (tokenStore != null) {
            Credentials c = tokenStore.load();
            if (c != null) {
                current = c;
                return true;
            }
        }
        return false;
    }

    // ── Internals ────────────────────────────────────────────────────

    /**
     * First-touch hydration: read disk, else run the OAuth flow. Holds the
     * refreshLock so concurrent first-touch callers don't all open browsers.
     */
    private Credentials loadOrAuth() throws DupeDBException {
        synchronized (refreshLock) {
            if (current != null) return current;
            if (tokenStore != null) {
                Credentials stored = tokenStore.load();
                if (stored != null) {
                    current = stored;
                    return stored;
                }
            }
            if (oAuthFlow != null) {
                Credentials fresh = oAuthFlow.authenticateAndExchange();
                current = fresh;
                if (tokenStore != null) {
                    try { tokenStore.save(fresh); } catch (IOException ignored) { /* best-effort */ }
                }
                return fresh;
            }
            throw new AuthException(
                "No authentication method configured. Provide a TokenStore + OAuthFlow, "
              + "or construct AuthManager with a direct token.");
        }
    }

    private Credentials refreshIfNeeded(Credentials seenCreds) throws DupeDBException {
        return refreshIfNeeded(seenCreds, false);
    }

    /**
     * Single-flight refresh per D-13:
     * <ol>
     *   <li>Acquire lock.</li>
     *   <li>Recheck {@code current} — if another thread already rotated, return.</li>
     *   <li>POST /oauth/token grant_type=refresh_token; on success, atomically swap {@code current} + persist.</li>
     *   <li>On {@code invalid_grant} failure, clear state and run a fresh OAuth flow per D-15.</li>
     * </ol>
     */
    private Credentials refreshIfNeeded(Credentials seenCreds, boolean force)
            throws DupeDBException {
        synchronized (refreshLock) {
            Credentials latest = current;
            // Double-check: if another thread already rotated in the window
            // between our lock acquisition and theirs, return the new creds.
            if (!force && latest != null && latest != seenCreds
                    && latest.expiresAt() != null
                    && latest.expiresAt().minusSeconds(REFRESH_WINDOW_SECONDS).isAfter(Instant.now())) {
                return latest;
            }

            String refreshTokenInUse = (latest != null) ? latest.refreshToken() : seenCreds.refreshToken();

            try {
                Credentials rotated = postRefresh(refreshTokenInUse);
                current = rotated;
                if (tokenStore != null) {
                    try { tokenStore.save(rotated); } catch (IOException ignored) {}
                }
                return rotated;
            } catch (OAuthException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("invalid_grant")) {
                    // D-15: refresh failed permanently → clear state + fresh OAuth flow
                    if (tokenStore != null) {
                        try { tokenStore.delete(); } catch (Exception ignored) {}
                    }
                    current = null;
                    if (oAuthFlow == null) {
                        throw new AuthException(
                            "Refresh failed and no OAuthFlow configured for fallback");
                    }
                    Credentials fresh = oAuthFlow.authenticateAndExchange();
                    current = fresh;
                    if (tokenStore != null) {
                        try { tokenStore.save(fresh); } catch (IOException ignored) {}
                    }
                    return fresh;
                }
                throw e;
            }
        }
    }

    /**
     * POST /oauth/token grant_type=refresh_token. Throws OAuthException
     * with a message containing the server's error code (e.g. "invalid_grant")
     * so the caller's branch can detect D-15's fallback condition.
     */
    private Credentials postRefresh(String refreshToken) throws OAuthException {
        if (baseUrl == null || oAuthFlow == null) {
            throw new OAuthException("Cannot refresh: no baseUrl/OAuthFlow configured (direct-token AuthManager?)");
        }
        String body = "grant_type=refresh_token"
            + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
            + "&client_id=" + URLEncoder.encode(oAuthFlow.getAppId(), StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/oauth/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(30))
            .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                TokenResponse tr = JsonHelper.fromJson(res.body(), TokenResponse.class);
                Instant expiresAt = Instant.now().plusSeconds(tr.expiresIn);
                Credentials existing = current;
                String appId = (existing != null) ? existing.appId() : oAuthFlow.getAppId();
                String createdAt = (existing != null) ? existing.createdAt() : Instant.now().toString();
                return new Credentials(
                    tr.accessToken,
                    appId,
                    createdAt,
                    tr.refreshToken,
                    expiresAt
                );
            }
            // Surface the server's error_code in the message so refreshIfNeeded
            // can detect "invalid_grant" via substring match.
            throw new OAuthException("Refresh failed: " + res.statusCode() + " " + res.body());
        } catch (IOException ioe) {
            throw new OAuthException("Refresh network error", ioe);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new OAuthException("Refresh interrupted", ie);
        }
    }

    /**
     * Internal /token response shape. Field names use camelCase; Gson is
     * configured with {@code LOWER_CASE_WITH_UNDERSCORES} naming policy
     * (see {@link JsonHelper}) so they map to {@code access_token},
     * {@code expires_in}, etc. on the wire. Mirrors OAuthFlow.TokenResponse.
     */
    private static class TokenResponse {
        String accessToken;
        String tokenType;
        long expiresIn;
        String refreshToken;
        String scope;
    }
}
