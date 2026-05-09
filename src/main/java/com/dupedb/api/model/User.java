package com.dupedb.api.model;

/**
 * Current user's profile from {@code /api/auth/me}.
 *
 * <p><b>Note:</b> {@code createdAt} is reserved for forward compatibility — the
 * backend currently does not populate it on this endpoint and it will always
 * be {@code null}. Reserved for a future backend release.</p>
 */
public record User(
    int id,
    String username,
    String displayName,
    String discordId,
    String discordUsername,
    String discordAvatar,
    String customAvatar,
    String role,
    String createdAt,
    boolean hideDiscordProfile
) {}
