package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * One match from the {@code /api/users/search?q=} fuzzy lookup used by the
 * credit-modal typeahead.
 *
 * <p>{@code discordId} and {@code discordAvatar} are {@code null} for users
 * with {@code hide_discord_profile = true} when the requester is not staff.
 * Snowflake-shaped queries (15-20 digit numeric) never return privacy-on
 * users to anyone — including staff — to avoid leaking Discord identity
 * through the credit-link source enrichment.
 */
public record UserSearchEntry(
    int id,
    String username,
    @SerializedName("display_name") String displayName,
    String role,
    @SerializedName("custom_avatar") String customAvatar,
    @SerializedName("discord_id") String discordId,
    @SerializedName("discord_avatar") String discordAvatar
) {}
