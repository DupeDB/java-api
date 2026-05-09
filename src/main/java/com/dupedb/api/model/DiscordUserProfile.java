package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Public profile data + points-system breakdown for a user, looked up by
 * Discord ID via {@code GET /api/users/discord/:discordId/profile}.
 *
 * <p>Wider shape than {@link UserProfile} (the by-user-id variant) — this one
 * carries the full v5 points-system breakdown including the "consolation"
 * tier (server-specific exploits whose verification ping failed the player
 * gate) and the gated counts that drive the Discord bot leaderboard.
 *
 * <p>{@code leaderboardRank} is {@code null} when {@code points == 0}
 * (excluded from rank computation).
 *
 * <p>Returns 404 (not 403) for users with {@code hide_discord_profile = true},
 * even for staff callers — Discord-ID lookup is treated as a privacy hard-gate.
 */
public record DiscordUserProfile(
    int id,
    String username,
    @SerializedName("display_name") String displayName,
    @SerializedName("created_at") String createdAt,
    @SerializedName("exploit_count") int exploitCount,
    @SerializedName("verified_exploit_count") int verifiedExploitCount,
    @SerializedName("verified_found_count") int verifiedFoundCount,
    @SerializedName("verified_sighting_count") int verifiedSightingCount,
    @SerializedName("credited_exploit_count") int creditedExploitCount,
    @SerializedName("verified_exploit_count_gated") int verifiedExploitCountGated,
    @SerializedName("verified_found_count_gated") int verifiedFoundCountGated,
    @SerializedName("verified_sighting_count_gated") int verifiedSightingCountGated,
    @SerializedName("non_author_credited_count") int nonAuthorCreditedCount,
    @SerializedName("self_credited_submit_only_count") int selfCreditedSubmitOnlyCount,
    @SerializedName("consolation_exploit_pts") double consolationExploitPts,
    @SerializedName("consolation_sighting_pts") double consolationSightingPts,
    double points,
    @SerializedName("leaderboard_rank") Integer leaderboardRank,
    @SerializedName("total_upvotes") int totalUpvotes,
    @SerializedName("total_views") int totalViews
) {}
