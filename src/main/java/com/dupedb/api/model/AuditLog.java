package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * A staff audit log entry from {@code GET /api/new-audit-logs?since=<ISO>}.
 *
 * <p>{@code details} is the raw JSON payload string the server stored when the
 * action was logged (varies per {@code action} + {@code category}). Parse it
 * yourself if you need structured access — the SDK leaves it untyped because
 * the schema is action-specific.
 *
 * <p>{@code actorDiscordId} is joined from the actor's user row; {@code null}
 * for actions taken by deleted users (LEFT JOIN nullable).
 */
public record AuditLog(
    int id,
    String action,
    String category,
    @SerializedName("actor_id") Integer actorId,
    @SerializedName("actor_username") String actorUsername,
    @SerializedName("target_type") String targetType,
    @SerializedName("target_id") String targetId,
    @SerializedName("target_name") String targetName,
    String details,
    @SerializedName("created_at") String createdAt,
    @SerializedName("actor_discord_id") String actorDiscordId
) {}
