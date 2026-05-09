package com.dupedb.api.model;

/**
 * Pagination metadata for paginated API responses.
 *
 * <p>{@code limit} is the page size that was used for the request. Some endpoints
 * (e.g. {@code /api/public/exploits}) hard-code a page size and return {@code 0}
 * here; callers should not assume non-zero.</p>
 */
public record Pagination(
    int page,
    int pages,
    int total,
    boolean hasMore,
    int limit
) {}
