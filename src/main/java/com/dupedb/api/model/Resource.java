package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * A resource entry (guide, mod, article, etc.).
 *
 * <p>{@code category} is one of the fixed slugs {@code "documentation"},
 * {@code "video"}, or {@code "mods"} — the dynamic category table (with
 * {@code categoryId}/{@code categoryName}) was retired in June 2026.
 */
public record Resource(
    int id,
    String title,
    String slug,
    String description,
    String content,
    String category,
    @SerializedName("resourceType") String resourceType,
    @SerializedName("externalUrl") String externalUrl,
    @SerializedName("iconUrl") String iconUrl,
    @SerializedName("bannerUrl") String bannerUrl,
    @SerializedName("isPublished") boolean isPublished,
    @SerializedName("createdBy") int createdBy,
    @SerializedName("authorUsername") String authorUsername,
    @SerializedName("authorDisplayName") String authorDisplayName,
    @SerializedName("authorDiscordId") String authorDiscordId,
    @SerializedName("authorDiscordAvatar") String authorDiscordAvatar,
    @SerializedName("authorCustomAvatar") String authorCustomAvatar,
    @SerializedName("authorRole") String authorRole,
    @SerializedName("createdAt") String createdAt,
    @SerializedName("updatedAt") String updatedAt
) {}
