package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.DiscordContributionsResult;
import com.dupedb.api.model.DiscordPointsList;
import com.dupedb.api.model.DiscordUserProfile;
import com.dupedb.api.model.UserLookup;
import com.dupedb.api.model.UserProfile;
import com.dupedb.api.model.UserSearchResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * API client for viewing other users' profiles ({@code /api/users}).
 * Separate from {@link UserApi} which manages the current user's own profile.
 */
public class UserProfileApi {
    private final HttpExecutor http;

    public UserProfileApi(HttpExecutor http) {
        this.http = http;
    }

    /** Gets a user's public profile. Calls {@code GET /api/users/:id/profile}. */
    public UserProfile getProfile(int userId) throws DupeDBException {
        return http.get("/api/users/" + userId + "/profile", UserProfile.class);
    }

    /** Looks up a user by username or display name (case-insensitive). Calls {@code GET /api/users/lookup/:name}. */
    public UserLookup lookup(String name) throws DupeDBException {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        return http.get("/api/users/lookup/" + encoded, UserLookup.class);
    }

    /**
     * Looks up a user by Discord ID. Calls
     * {@code GET /api/users/discord/:discordId/profile}.
     *
     * <p>Returns the wider {@link DiscordUserProfile} shape with the full v5
     * points-system breakdown including consolation tiers and gated counts.
     *
     * @param discordId the Discord snowflake (15-20 digit numeric string)
     * @throws com.dupedb.api.exception.ApiException 400 if the Discord ID
     *     format is invalid; 404 if no DupeDB user has that Discord ID, or
     *     the user has {@code hide_discord_profile} enabled (privacy hard-gate).
     */
    public DiscordUserProfile getProfileByDiscordId(String discordId) throws DupeDBException {
        String encoded = URLEncoder.encode(discordId, StandardCharsets.UTF_8);
        return http.get("/api/users/discord/" + encoded + "/profile", DiscordUserProfile.class);
    }

    /**
     * Lists per-row contributions (verified exploits authored, credits, and
     * sightings) that built the user's points total. Calls
     * {@code GET /api/users/discord/:discordId/contributions?limit=&offset=}.
     *
     * @param discordId Discord snowflake (15-20 digit)
     * @param limit     1-50, server caps
     * @param offset    >= 0
     */
    public DiscordContributionsResult getContributionsByDiscordId(String discordId, int limit, int offset)
            throws DupeDBException {
        String encoded = URLEncoder.encode(discordId, StandardCharsets.UTF_8);
        String path = "/api/users/discord/" + encoded + "/contributions"
            + "?limit=" + limit + "&offset=" + offset;
        return http.get(path, DiscordContributionsResult.class);
    }

    /** Convenience overload using the server defaults ({@code limit=10}, {@code offset=0}). */
    public DiscordContributionsResult getContributionsByDiscordId(String discordId) throws DupeDBException {
        return getContributionsByDiscordId(discordId, 10, 0);
    }

    /**
     * Returns the full Discord-points leaderboard (every user with
     * {@code points > 0} who has not enabled Discord profile privacy). Calls
     * {@code GET /api/users/discord-points}.
     *
     * <p>No pagination — the server emits the entire list sorted by
     * {@code points DESC}. Backed by the {@code mv_user_exploit_stats}
     * materialized view (refreshed every minute), so consecutive calls within
     * the refresh window return identical data.
     */
    public DiscordPointsList discordPoints() throws DupeDBException {
        return http.get("/api/users/discord-points", DiscordPointsList.class);
    }

    /**
     * Fuzzy user search used by the credit-modal typeahead. Calls
     * {@code GET /api/users/search?q=}. Server caps results at 8.
     *
     * <p>Matches on username, display_name, and (for 15-20 digit numeric
     * queries) Discord ID. Excludes banned users and system accounts.
     *
     * @param query 2-64 characters; queries shorter than 2 chars return an
     *              empty list, longer than 64 throws {@code 400}.
     */
    public UserSearchResult search(String query) throws DupeDBException {
        String encoded = URLEncoder.encode(query != null ? query : "", StandardCharsets.UTF_8);
        return http.get("/api/users/search?q=" + encoded, UserSearchResult.class);
    }
}
