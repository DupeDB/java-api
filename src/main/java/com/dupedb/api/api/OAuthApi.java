package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth 2.1 protocol surface. Currently exposes RFC 7009 token revocation;
 * future protocol additions (e.g. introspection) belong here.
 *
 * <p>Distinct from {@link UserApi} (which manages the signed-in user's
 * profile-level resources) and {@link MyAppsApi} (self-service app CRUD).</p>
 */
public class OAuthApi {
    private final HttpExecutor http;

    public OAuthApi(HttpExecutor http) {
        this.http = http;
    }

    /**
     * Revokes an OAuth token (refresh or access). RFC 7009 §2.2 semantics:
     * the server always returns 200, even on unknown tokens, to prevent token
     * existence enumeration. This method therefore never throws on a "not
     * found" response.
     *
     * <p>The request is sent as {@code application/x-www-form-urlencoded} per
     * RFC 7009 §2.1. When {@code tokenTypeHint} is non-null, the server
     * matches that token store first; otherwise it tries refresh first, then
     * access.</p>
     *
     * @param token         the token string to revoke
     * @param tokenTypeHint optional RFC 7009 hint, e.g. {@code "refresh_token"}
     *                      or {@code "access_token"}; pass {@code null} when
     *                      unknown
     */
    public void revoke(String token, String tokenTypeHint) throws DupeDBException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("token", token);
        if (tokenTypeHint != null) form.put("token_type_hint", tokenTypeHint);
        http.postForm("/api/oauth/revoke", form, Void.class);
    }
}
