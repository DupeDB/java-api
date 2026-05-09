package com.dupedb.api.model;

import java.util.List;

/**
 * Response shape for {@code PATCH /api/oauth/my-apps/:id} — the updated
 * {@link OAuthApp} fields flattened alongside cascade metadata.
 *
 * <p>{@code revokedCount} is the number of access + refresh tokens that were
 * revoked as part of an edit-cascade (only triggered when {@code redirectUris}
 * changed).</p>
 *
 * <p>{@code changedFields} lists the fields that actually changed
 * (e.g. {@code ["name", "redirectUris"]}); empty list on no-op PATCH.</p>
 */
public record OAuthAppPatchResult(
    String id,
    String name,
    List<String> redirectUris,
    Integer ownerUserId,
    String createdAt,
    boolean readOnly,
    int revokedCount,
    List<String> changedFields
) {}
