package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * One row from the {@code /api/users/discord-points} leaderboard. Includes
 * every user with {@code points > 0} who has not enabled
 * {@code hide_discord_profile}, sorted by {@code points DESC}.
 */
public record DiscordPointsEntry(
    @SerializedName("discord_id") String discordId,
    String username,
    @SerializedName("display_name") String displayName,
    @SerializedName("verified_exploit_count") int verifiedExploitCount,
    @SerializedName("verified_found_count") int verifiedFoundCount,
    @SerializedName("verified_sighting_count") int verifiedSightingCount,
    @SerializedName("credited_exploit_count") int creditedExploitCount,
    double points,
    @SerializedName("total_upvotes") int totalUpvotes,
    @SerializedName("total_views") int totalViews
) {}
