package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * One row from {@code GET /api/users/discord/:discordId/contributions}: a
 * single verified exploit, credit, or sighting that contributed to the user's
 * v9 points total.
 *
 * <p>{@code kind} is one of:
 * <ul>
 *   <li>{@code "exploit"} — the user authored a verified exploit</li>
 *   <li>{@code "credit"} — the user was credited (via Discord URL in
 *       {@code sources}) on a verified exploit they did not author</li>
 *   <li>{@code "sighting"} — the user reported a verified server sighting</li>
 * </ul>
 *
 * <p>{@code points = basePts + bonusPts}. The per-signal bonus columns
 * ({@code viewBonus}, {@code upvoteBonus}, {@code playerBonus},
 * {@code downloadBonus}) break {@code bonusPts} down by source signal.
 *
 * <p>{@code tier} is always {@code "full"} since the v9 points overhaul —
 * the v5 {@code "consolation"} tier is retired (kept as a field for
 * forward compatibility).
 *
 * <p>{@code role} is one of {@code "author_found"},
 * {@code "author_self_credit"}, {@code "author_submit_only"},
 * {@code "non_author_credit"}, or {@code "sighting"}.
 */
public record Contribution(
    String kind,
    String id,
    @SerializedName("exploit_id") String exploitId,
    @SerializedName("comment_id") Integer commentId,
    String name,
    @SerializedName("server_ip") String serverIp,
    String date,
    double points,
    String tier,
    String role,
    @SerializedName("base_pts") double basePts,
    @SerializedName("bonus_pts") double bonusPts,
    @SerializedName("view_bonus") double viewBonus,
    @SerializedName("upvote_bonus") double upvoteBonus,
    @SerializedName("player_bonus") double playerBonus,
    @SerializedName("download_bonus") double downloadBonus
) {}
