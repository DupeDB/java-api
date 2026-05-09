package com.dupedb.api.model;

import java.util.List;

/**
 * Response from {@code GET /api/users/discord/:discordId/contributions}.
 *
 * <p>{@code total} is the count of all rows (across all pages); {@code limit}
 * and {@code offset} echo the pagination parameters back. The points returned
 * by summing {@link Contribution#points()} across all pages should equal the
 * {@code points} field on the user's {@link DiscordUserProfile}.
 */
public record DiscordContributionsResult(
    int total,
    int limit,
    int offset,
    List<Contribution> contributions
) {}
