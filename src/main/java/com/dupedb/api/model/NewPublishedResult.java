package com.dupedb.api.model;

import java.util.List;

/**
 * Response from {@code GET /api/new-published?since=<ISO>}.
 *
 * <p>Polling-friendly: store the most-recent {@link ExploitCard#publishedAt()}
 * (or the {@link #since()} you sent) and pass it back as the next request's
 * {@code since} cursor. Server caps each call at 100 entries and only returns
 * entries with {@code status = 'verified'} and {@code notify_discord != 0}.
 */
public record NewPublishedResult(
    List<ExploitCard> exploits,
    int count,
    String since
) {}
