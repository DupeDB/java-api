package com.dupedb.api.model;

import java.util.List;

/**
 * Response from {@code GET /api/users/search?q=}. Server caps the result list
 * at 8 entries (typeahead-sized) and returns an empty array for queries
 * shorter than 2 characters.
 */
public record UserSearchResult(
    List<UserSearchEntry> users
) {}
