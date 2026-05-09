package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.OAuthApp;
import com.dupedb.api.model.OAuthAppListResponse;
import com.dupedb.api.model.OAuthAppPatchResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-service OAuth app management for the authenticated user
 * ({@code /api/oauth/my-apps}). Surface introduced in backend Phase 105.
 *
 * <p>All endpoints require authentication. The user can manage only apps they
 * own; cross-owner attempts return 403 / 404 indistinguishably (intentional —
 * see Phase 105 threat model T-105-12).</p>
 */
public class MyAppsApi {
    private final HttpExecutor http;

    public MyAppsApi(HttpExecutor http) {
        this.http = http;
    }

    /**
     * Lists this user's owned OAuth apps and the per-user quota.
     * {@code GET /api/oauth/my-apps}.
     */
    public OAuthAppListResponse list() throws DupeDBException {
        return http.get("/api/oauth/my-apps", OAuthAppListResponse.class);
    }

    /**
     * Creates a new owned OAuth app. {@code POST /api/oauth/my-apps}.
     *
     * <p>App ID must be lowercase alphanumeric with dashes only, 3-32 chars.
     * App name 1-100 chars (NFKC-normalized server-side). At least one
     * redirect URI is required.</p>
     *
     * @throws com.dupedb.api.exception.ApiException 400 with message
     *     {@code "reserved_name"} if {@code id} matches a reserved prefix
     *     (dupedb, admin, staff, mod, moderator, official, system, bot, or
     *     a CSV-configured extra). Inspect {@link com.dupedb.api.exception.ApiException#getMessage()}
     *     to disambiguate from generic {@code "invalid_request"}.
     * @throws com.dupedb.api.exception.RateLimitException 429 — either
     *     {@code quota_exceeded} (per-user app limit reached, default 5) or
     *     the per-user create rate limit (3/hr).
     */
    public OAuthApp create(String id, String name, List<String> redirectUris, boolean readOnly)
            throws DupeDBException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("name", name);
        body.put("redirectUris", redirectUris);
        body.put("readOnly", readOnly);
        return http.post("/api/oauth/my-apps", body, OAuthApp.class);
    }

    /**
     * Updates an owned OAuth app. {@code PATCH /api/oauth/my-apps/:id}.
     *
     * <p>Any argument passed as {@code null} is omitted from the request body
     * (the backend treats absent fields as no-op). To change a value, pass it;
     * to leave it untouched, pass null.</p>
     *
     * <p>If {@code redirectUris} is changed, all active access + refresh
     * tokens for this app are revoked across all users (Phase 105 D-07
     * cascade). The response includes {@code revokedCount} and
     * {@code changedFields} so callers can surface this to the user.</p>
     */
    public OAuthAppPatchResult update(String id, String name, List<String> redirectUris, Boolean readOnly)
            throws DupeDBException {
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (redirectUris != null) body.put("redirectUris", redirectUris);
        if (readOnly != null) body.put("readOnly", readOnly);
        return http.patch("/api/oauth/my-apps/" + id, body, OAuthAppPatchResult.class);
    }

    /**
     * Deletes an owned OAuth app. Cascades token revocation via the FK ON
     * DELETE CASCADE chain. {@code DELETE /api/oauth/my-apps/:id}.
     */
    public void delete(String id) throws DupeDBException {
        http.delete("/api/oauth/my-apps/" + id);
    }
}
