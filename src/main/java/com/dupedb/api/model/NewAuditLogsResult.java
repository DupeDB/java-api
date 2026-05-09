package com.dupedb.api.model;

import java.util.List;

/**
 * Response from {@code GET /api/new-audit-logs?since=<ISO>}.
 *
 * <p>Polling-friendly: store the most-recent {@link AuditLog#createdAt()} and
 * pass it back as the next request's {@code since} cursor. Server caps each
 * call at 500 entries (higher than the 100-cap polling endpoints because audit
 * volume can spike during moderation pushes).
 */
public record NewAuditLogsResult(
    List<AuditLog> logs,
    int count,
    String since
) {}
