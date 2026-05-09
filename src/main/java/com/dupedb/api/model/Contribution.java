package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * One row from {@code GET /api/users/discord/:discordId/contributions}: a
 * single verified exploit, credit, or sighting that contributed to the user's
 * v5 points total.
 *
 * <p>{@code kind} is one of:
 * <ul>
 *   <li>{@code "exploit"} — the user authored a verified exploit</li>
 *   <li>{@code "credit"} — the user was credited (via Discord URL in
 *       {@code sources}) on a verified exploit they did not author</li>
 *   <li>{@code "sighting"} — the user reported a verified server sighting</li>
 * </ul>
 *
 * <p>{@code tier} is {@code "full"} or {@code "consolation"} (server-specific
 * exploits whose verification-time player-count ping fell below the gate get
 * the consolation tier with a 0.1 point payout).
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
    String role
) {}
