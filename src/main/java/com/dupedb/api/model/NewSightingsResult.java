package com.dupedb.api.model;

import java.util.List;

/**
 * Response from {@code GET /api/new-unverified-sightings?since=<ISO>} and
 * {@code GET /api/new-verified-sightings?since=<ISO>}.
 *
 * <p>Polling-friendly: store the most-recent {@link BotSighting#createdAt()}
 * (unverified feed) or {@link BotSighting#verifiedAt()} (verified feed) and
 * pass it back as the next request's {@code since} cursor. Server caps each
 * call at 100 entries.
 */
public record NewSightingsResult(
    List<BotSighting> sightings,
    int count,
    String since
) {}
