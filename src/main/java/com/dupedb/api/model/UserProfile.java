package com.dupedb.api.model;

/** Another user's public profile. Discord fields are nullable and respect privacy settings. */
public record UserProfile(
    int id,
    String username,
    String displayName,
    String role,
    String customAvatar,
    String createdAt,
    int exploitCount,
    int totalUpvotes,
    int totalViews,
    String discordId,
    String discordAvatar,
    boolean isBanned,
    String timeoutUntil,
    boolean isSystemAccount,
    String learnMoreUrl
) {}
