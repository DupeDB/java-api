package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * A connected OAuth application the user has authorized.
 *
 * <p>{@code id} is the refresh-token family UUID per OAuth 2.1 cutover (Phase
 * 103) — previously a numeric SERIAL on the legacy app_tokens table. Use
 * {@link #appId} for the OAuth client id to call /api/oauth/connected DELETE.</p>
 */
public record ConnectedApp(
    String id,
    @SerializedName("appId") String appId,
    @SerializedName("appName") String appName,
    @SerializedName("readOnly") boolean readOnly,
    @SerializedName("createdAt") String createdAt,
    @SerializedName("lastUsedAt") String lastUsedAt
) {}
