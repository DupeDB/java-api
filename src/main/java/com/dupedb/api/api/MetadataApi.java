package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.ExploitCard;
import com.dupedb.api.model.ExploitCardBatchResult;
import com.dupedb.api.model.HealthStatus;
import com.dupedb.api.model.LatestActivity;
import com.dupedb.api.model.NewAuditLogsResult;
import com.dupedb.api.model.NewPublishedResult;
import com.dupedb.api.model.NewReportsResult;
import com.dupedb.api.model.NewSightingsResult;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

    /**
     * Lists verified exploits published since the given timestamp (max 100 per call).
     * Calls {@code GET /api/new-published?since=<ISO>}. Requires authentication.
     *
     * <p>Useful for polling integrations: store the most-recent {@code publishedAt}
     * you've processed and pass it back on the next call. Server only returns entries
     * with {@code status = 'verified'} and {@code notify_discord != 0}.
     *
     * <p>The route also supports a staff-issued API key via the {@code X-API-Key}
     * header; this SDK uses normal Bearer auth. For higher-throughput integrations
     * a staff API key is more appropriate — open a ticket in the DupeDB Discord.
     *
     * @param sinceIso ISO 8601 timestamp (e.g. {@code "2026-05-01T00:00:00Z"})
     */
    public NewPublishedResult newPublished(String sinceIso) throws DupeDBException {
        String encoded = URLEncoder.encode(sinceIso, StandardCharsets.UTF_8);
        return http.get("/api/new-published?since=" + encoded, NewPublishedResult.class);
    }

    /** Convenience overload that serializes {@code since} as ISO 8601. */
    public NewPublishedResult newPublished(Instant since) throws DupeDBException {
        return newPublished(since.toString());
    }

    /**
     * Fetches trimmed card-level fields for up to 100 exploits in one request.
     * Calls {@code GET /api/exploits-card-batch?ids=a,b,c}. Requires authentication.
     *
     * <p>Drafts and rejected exploits are filtered out server-side; the response
     * {@code count} may be less than {@code ids.size()}. Server caps each call at
     * 100 IDs (the SDK does not enforce this — pass more and the server will
     * silently slice). Originally added for Discord bot V2 container live updates.
     *
     * <p>An empty or {@code null} {@code ids} list short-circuits to an empty
     * result without hitting the network.
     */
    public ExploitCardBatchResult exploitCardBatch(List<String> ids) throws DupeDBException {
        if (ids == null || ids.isEmpty()) {
            return new ExploitCardBatchResult(List.of(), 0);
        }
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) csv.append(',');
            csv.append(URLEncoder.encode(ids.get(i), StandardCharsets.UTF_8));
        }
        return http.get("/api/exploits-card-batch?ids=" + csv, ExploitCardBatchResult.class);
    }

    /**
     * Lists unverified exploits submitted since the given timestamp (max 100
     * per call). Calls {@code GET /api/new-unverified?since=<ISO>}. Requires
     * authentication.
     *
     * <p>Wire shape mirrors {@link #newPublished(String)}; the only difference
     * is the {@code status = 'unverified'} filter on the server. Useful for
     * polling integrations that want to surface fresh submissions before
     * staff verification.
     *
     * @param sinceIso ISO 8601 timestamp (e.g. {@code "2026-05-01T00:00:00Z"})
     */
    public NewPublishedResult newUnverified(String sinceIso) throws DupeDBException {
        String encoded = URLEncoder.encode(sinceIso, StandardCharsets.UTF_8);
        return http.get("/api/new-unverified?since=" + encoded, NewPublishedResult.class);
    }

    /** Convenience overload that serializes {@code since} as ISO 8601. */
    public NewPublishedResult newUnverified(Instant since) throws DupeDBException {
        return newUnverified(since.toString());
    }

    /**
     * Lists newly created (still pending) sightings since the given timestamp
     * (max 100 per call). Calls {@code GET /api/new-unverified-sightings?since=<ISO>}.
     * Requires authentication.
     */
    public NewSightingsResult newUnverifiedSightings(String sinceIso) throws DupeDBException {
        String encoded = URLEncoder.encode(sinceIso, StandardCharsets.UTF_8);
        return http.get("/api/new-unverified-sightings?since=" + encoded, NewSightingsResult.class);
    }

    /** Convenience overload that serializes {@code since} as ISO 8601. */
    public NewSightingsResult newUnverifiedSightings(Instant since) throws DupeDBException {
        return newUnverifiedSightings(since.toString());
    }

    /**
     * Lists newly verified sightings since the given timestamp (max 100 per
     * call). Calls {@code GET /api/new-verified-sightings?since=<ISO>}.
     * Requires authentication.
     *
     * <p>The verified variant carries extra fields (
     * {@code verifiedPlayerCount}, {@code playerGateQualifies},
     * {@code serverIcon}) on each {@link com.dupedb.api.model.BotSighting}
     * that the unverified endpoint omits.
     */
    public NewSightingsResult newVerifiedSightings(String sinceIso) throws DupeDBException {
        String encoded = URLEncoder.encode(sinceIso, StandardCharsets.UTF_8);
        return http.get("/api/new-verified-sightings?since=" + encoded, NewSightingsResult.class);
    }

    /** Convenience overload that serializes {@code since} as ISO 8601. */
    public NewSightingsResult newVerifiedSightings(Instant since) throws DupeDBException {
        return newVerifiedSightings(since.toString());
    }

    /**
     * Lists exploit reports submitted since the given timestamp (max 100 per
     * call). Calls {@code GET /api/new-reports?since=<ISO>}. Requires
     * authentication.
     */
    public NewReportsResult newReports(String sinceIso) throws DupeDBException {
        String encoded = URLEncoder.encode(sinceIso, StandardCharsets.UTF_8);
        return http.get("/api/new-reports?since=" + encoded, NewReportsResult.class);
    }

    /** Convenience overload that serializes {@code since} as ISO 8601. */
    public NewReportsResult newReports(Instant since) throws DupeDBException {
        return newReports(since.toString());
    }

    /**
     * Lists audit log entries created after the given timestamp (max 500 per
     * call). Calls {@code GET /api/new-audit-logs?since=<ISO>}. Requires
     * authentication; API-key callers must hold the {@code read_audit_logs}
     * scope.
     *
     * <p>Higher per-call cap than the other polling endpoints because audit
     * volume can spike during moderation pushes.
     */
    public NewAuditLogsResult newAuditLogs(String sinceIso) throws DupeDBException {
        String encoded = URLEncoder.encode(sinceIso, StandardCharsets.UTF_8);
        return http.get("/api/new-audit-logs?since=" + encoded, NewAuditLogsResult.class);
    }

    /** Convenience overload that serializes {@code since} as ISO 8601. */
    public NewAuditLogsResult newAuditLogs(Instant since) throws DupeDBException {
        return newAuditLogs(since.toString());
    }
}
