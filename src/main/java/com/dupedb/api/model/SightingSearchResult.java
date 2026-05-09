package com.dupedb.api.model;

import java.util.List;

/**
 * Response from {@code GET /api/sightings/search}.
 *
 * <p>Note: unlike {@link SearchResult}, the sighting browse endpoint does not
 * return a nested {@link Pagination} envelope. The four fields are emitted at
 * the top level by the server's {@code searchSightings()} service; clients
 * compute {@code pages = ceil(total / limit)} themselves if needed.
 */
public record SightingSearchResult(
    List<BrowseSighting> sightings,
    int total,
    int page,
    boolean hasMore
) {}
