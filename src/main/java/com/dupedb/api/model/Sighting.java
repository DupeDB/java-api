package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * A community-reported server sighting of an exploit. Returned by
 * {@code GET /api/auth/my-sightings}.
 *
 * <p>{@code isVerified}, {@code isPatched} and {@code isRejected} are {@code 0} (no)
 * or {@code 1} (yes). {@code rejectionReason} is null unless the sighting was rejected
 * by staff; rejected sightings never become verified.
 */
public record Sighting(
    int id,
    @SerializedName("server_ip") String serverIp,
    @SerializedName("is_verified") int isVerified,
    @SerializedName("verified_at") String verifiedAt,
    @SerializedName("is_patched") int isPatched,
    @SerializedName("patched_at") String patchedAt,
    @SerializedName("is_rejected") int isRejected,
    @SerializedName("rejection_reason") String rejectionReason,
    @SerializedName("created_at") String createdAt,
    @SerializedName("comment_id") int commentId,
    String content,
    @SerializedName("exploit_id") String exploitId,
    @SerializedName("exploit_name") String exploitName,
    @SerializedName("reply_count") int replyCount
) {}
