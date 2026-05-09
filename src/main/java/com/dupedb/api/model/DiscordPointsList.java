package com.dupedb.api.model;

import java.util.List;

/**
 * Response from {@code GET /api/users/discord-points} — the full points
 * leaderboard used by the Discord bot for role sync and the {@code /points}
 * page-1 view. No pagination; the server returns all eligible users in one
 * shot (sorted by {@code points DESC}).
 */
public record DiscordPointsList(
    List<DiscordPointsEntry> users
) {}
