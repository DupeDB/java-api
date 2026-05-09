package com.dupedb.api.model;

import java.util.List;

/**
 * Response from {@code GET /api/new-reports?since=<ISO>}.
 *
 * <p>Polling-friendly: store the most-recent {@link Report#createdAt()} and
 * pass it back as the next request's {@code since} cursor. Server caps each
 * call at 100 entries.
 */
public record NewReportsResult(
    List<Report> reports,
    int count,
    String since
) {}
