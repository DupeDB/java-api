package com.dupedb.api.auth;

import java.time.Instant;

/**
 * OAuth 2.1 credentials persisted to disk by {@link TokenStore} and held
 * in memory by {@link AuthManager}.
 *
 * <p>v2.0.0: renamed {@code token} → {@code accessToken}; added
 * {@code refreshToken} and {@code expiresAt} fields per Phase 103 D-16.
 * 1.x credentials files become unparseable on first read (the renamed
 * field key) → invalidated automatically.</p>
 *
 * @param accessToken  OAuth 2.1 bearer access token (raw, base64url, no prefix)
 * @param appId        the OAuth app identifier that issued these credentials
 * @param createdAt    ISO-8601 timestamp of when these credentials were saved
 * @param refreshToken OAuth 2.1 refresh token (raw, base64url, used by AuthManager
 *                     for single-flight rotation per Phase 103 D-13)
 * @param expiresAt    UTC instant at which {@code accessToken} expires; AuthManager
 *                     proactively refreshes 60s before this time per D-13
 */
public record Credentials(
    String accessToken,
    String appId,
    String createdAt,
    String refreshToken,
    Instant expiresAt
) {}
