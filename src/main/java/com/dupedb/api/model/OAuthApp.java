package com.dupedb.api.model;

import java.util.List;

/**
 * OAuth application (owner-scoped view).
 *
 * <p>Returned by the self-service surface ({@code GET /api/oauth/my-apps} list
 * items, {@code POST /api/oauth/my-apps}) and by single-record admin endpoints
 * ({@code GET /api/oauth/apps/:id}, {@code POST /api/oauth/apps}).</p>
 */
public record OAuthApp(
    String id,
    String name,
    List<String> redirectUris,
    Integer ownerUserId,
    String createdAt,
    boolean readOnly
) {}
