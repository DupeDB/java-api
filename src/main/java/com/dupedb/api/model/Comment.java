package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * A comment on an exploit.
 *
 * <p>{@code isSighting} is {@code 0} (normal comment) or {@code 1} (server-sighting comment).
 * Sighting comments carry a {@code sightingServerIp} and verification metadata.
 *
 * <p>{@code replies} contains nested replies in DESC order (newest first). Server clamps
 * threading depth to 3 levels — replies beyond that are flattened into the parent's parent.
 */
public record Comment(
    int id,
    String exploitId,
    String author,
    int authorUserId,
    String content,
    String datePosted,
    String authorDisplayName,
    String discordId,
    String discordAvatar,
    String customAvatar,
    String authorRole,
    @SerializedName("parent_comment_id") Integer parentCommentId,
    @SerializedName("is_sighting") int isSighting,
    List<Comment> replies,
    @SerializedName("sighting_id") Integer sightingId,
    @SerializedName("sighting_server_ip") String sightingServerIp,
    @SerializedName("sighting_verified") Integer sightingVerified,
    @SerializedName("sighting_patched") Integer sightingPatched
) {}
