package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * A sighting row from one of the polling endpoints
 * ({@code /api/new-unverified-sightings}, {@code /api/new-verified-sightings}).
 *
 * <p>Different shape from {@link Sighting} (per-user own list) and
 * {@link BrowseSighting} (cross-exploit browse list) — this one is denormalized
 * for Discord bot consumption and joins the comment author + content directly.
 *
 * <p>The verified variant carries extra fields ({@code verifiedPlayerCount},
 * {@code playerGateQualifies}, {@code serverIcon}) that the unverified
 * variant omits; those will be {@code null} on rows from the unverified feed.
 * Conversely {@code createdAt} is populated only by the unverified feed and
 * {@code verifiedAt} only by the verified feed — check whichever endpoint
 * returned the row.
 */
public record BotSighting(
    @SerializedName("sighting_id") int sightingId,
    @SerializedName("server_ip") String serverIp,
    @SerializedName("created_at") String createdAt,
    @SerializedName("verified_at") String verifiedAt,
    @SerializedName("comment_id") int commentId,
    @SerializedName("exploit_id") String exploitId,
    @SerializedName("exploit_name") String exploitName,
    String author,
    String content,
    @SerializedName("verified_player_count") Integer verifiedPlayerCount,
    @SerializedName("player_gate_qualifies") Integer playerGateQualifies,
    @SerializedName("server_icon") String serverIcon,
    /**
     * Discord-bot control flag (verified feed only): {@code 0} → suppress the
     * custom-message prefix + role ping. {@code null} on the unverified feed.
     */
    @SerializedName("notify_with_mention") Integer notifyWithMention
) {}
