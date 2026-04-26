package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.ExploitCard;
import com.dupedb.api.model.HealthStatus;
import com.dupedb.api.model.LatestActivity;
import com.dupedb.api.model.Plugin;
import com.dupedb.api.model.PublicStats;
import com.dupedb.api.model.SearchResult;
import com.dupedb.api.model.ServerIpResult;
import com.dupedb.api.model.SiteStats;
import com.dupedb.api.model.SiteVisibility;
import com.dupedb.api.model.StatsSnapshot;
import com.dupedb.api.model.Tag;
import com.dupedb.api.model.VersionInfo;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/** API client for site metadata endpoints ({@code /api}). */
public class MetadataApi {
    private final HttpExecutor http;

    public MetadataApi(HttpExecutor http) {
        this.http = http;
    }

    /** Checks API server health. Calls {@code GET /api/health}. No auth required. */
    public HealthStatus health() throws DupeDBException {
        return http.get("/api/health", HealthStatus.class);
    }

    /** Gets site version info. Calls {@code GET /api/version}. No auth required. */
    public VersionInfo version() throws DupeDBException {
        return http.get("/api/version", VersionInfo.class);
    }

    /** Lists all tags with exploit counts. Calls {@code GET /api/tags}. Requires authentication. */
    public List<Tag> tags() throws DupeDBException {
        Type type = new TypeToken<List<Tag>>() {}.getType();
        return http.get("/api/tags", type);
    }

    /** Lists Minecraft versions referenced by exploits. Calls {@code GET /api/versions}. Requires authentication. */
    public List<String> versions() throws DupeDBException {
        record VersionsResponse(List<String> versions) {}
        VersionsResponse response = http.get("/api/versions", VersionsResponse.class);
        return response != null ? response.versions() : List.of();
    }

    /** Lists exploit types. Calls {@code GET /api/types}. Requires authentication. */
    public List<String> types() throws DupeDBException {
        Type type = new TypeToken<List<String>>() {}.getType();
        return http.get("/api/types", type);
    }

    /**
     * @deprecated Server returns empty array. Use server software fields on exploits instead.
     * Requires authentication.
     */
    @Deprecated
    public List<String> serverTypes() throws DupeDBException {
        Type type = new TypeToken<List<String>>() {}.getType();
        return http.get("/api/server-types", type);
    }

    /** Gets site-wide statistics. Calls {@code GET /api/stats}. Requires authentication. */
    public SiteStats stats() throws DupeDBException {
        return http.get("/api/stats", SiteStats.class);
    }

    /** Gets the most recent exploit activity, or null. Calls {@code GET /api/latest-activity}. Requires authentication. */
    public LatestActivity latestActivity() throws DupeDBException {
        record ActivityResponse(LatestActivity activity) {}
        ActivityResponse response = http.get("/api/latest-activity", ActivityResponse.class);
        return response != null ? response.activity() : null;
    }

    /** Lists server IPs referenced by exploits and verified sightings. Calls {@code GET /api/server-ips}. Requires authentication. */
    public ServerIpResult serverIps(int page, int limit) throws DupeDBException {
        return http.get("/api/server-ips?page=" + page + "&limit=" + limit, ServerIpResult.class);
    }

    /** Lists server IPs (first page, default limit of 50). Requires authentication. */
    public ServerIpResult serverIps() throws DupeDBException {
        return serverIps(1, 50);
    }

    /** Lists plugins referenced by exploits. Calls {@code GET /api/plugins}. Requires authentication. */
    public List<Plugin> plugins() throws DupeDBException {
        Type type = new TypeToken<List<Plugin>>() {}.getType();
        return http.get("/api/plugins", type);
    }

    /** Gets public aggregate statistics. Calls {@code GET /api/public/stats}. No auth required. */
    public PublicStats publicStats() throws DupeDBException {
        return http.get("/api/public/stats", PublicStats.class);
    }

    /**
     * Gets public stats history. Calls {@code GET /api/public/stats/history}. No auth required.
     * @param days number of days (1-90)
     */
    public List<StatsSnapshot> publicStatsHistory(int days) throws DupeDBException {
        Type type = new TypeToken<List<StatsSnapshot>>() {}.getType();
        return http.get("/api/public/stats/history?days=" + days, type);
    }

    /** Gets public stats history for the last 30 days. */
    public List<StatsSnapshot> publicStatsHistory() throws DupeDBException {
        return publicStatsHistory(30);
    }

    /** Gets public feature visibility flags. Calls {@code GET /api/site-visibility}. No auth required. */
    public SiteVisibility siteVisibility() throws DupeDBException {
        return http.get("/api/site-visibility", SiteVisibility.class);
    }

    /** Gets newest public exploit cards (up to 10). Calls {@code GET /api/public/exploits}. No auth required. */
    public SearchResult<ExploitCard> publicExploits() throws DupeDBException {
        Type type = new TypeToken<SearchResult<ExploitCard>>() {}.getType();
        return http.get("/api/public/exploits", type);
    }
}
