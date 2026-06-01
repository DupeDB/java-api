package com.dupedb.api.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Type-safe builder for {@link SightingApi#search(String, int, SightingFilters)}
 * filter parameters — the API equivalent of the site's community sightings
 * filter sidebar.
 *
 * <p>All setters return {@code this} for chaining. Call {@link #toMap()} to
 * materialize a query-string-ready {@code Map}, or pass the builder directly to
 * {@link SightingApi#search(String, int, SightingFilters)}.
 *
 * <p><b>Sort/order pairings.</b> The server only honors a fixed set of
 * sort+order combinations (others fall back to {@code date_submitted} /
 * {@code desc}):
 * <ul>
 *   <li>{@code date_submitted} — {@code asc} or {@code desc}</li>
 *   <li>{@code verified_player_count} — {@code desc} only ("Most Players")</li>
 *   <li>{@code server_ip} — {@code asc} only ("IP A-Z")</li>
 * </ul>
 * When a non-empty query is supplied to {@code search}, the server ranks by
 * match relevance and ignores {@code sort} (mirrors the frontend hiding the
 * sort control during a text search).
 */
public final class SightingFilters {
    private String status;
    private String serverIp;
    private Integer playerMin;
    private Integer playerMax;
    private String sort;
    private String order;
    private Integer limit;

    /** Filter by sighting status: {@code working}, {@code patched} (comma-separated for multi-select). */
    public SightingFilters status(String status) { this.status = status; return this; }

    /** Filter by server IP (comma-separated for multi-select; matches any of the listed IPs). */
    public SightingFilters serverIp(String serverIp) { this.serverIp = serverIp; return this; }

    /** Inclusive lower bound on verified player count. Servers with no recorded count are excluded. */
    public SightingFilters playerMin(int playerMin) { this.playerMin = playerMin; return this; }

    /** Inclusive upper bound on verified player count. Servers with no recorded count are excluded. */
    public SightingFilters playerMax(int playerMax) { this.playerMax = playerMax; return this; }

    /** Sort field: {@code date_submitted}, {@code verified_player_count}, or {@code server_ip}. See the class doc for valid order pairings. */
    public SightingFilters sort(String sort) { this.sort = sort; return this; }

    /** Sort order: {@code asc} or {@code desc} (constrained per sort field — see class doc). */
    public SightingFilters order(String order) { this.order = order; return this; }

    /** Results per page (1-100). */
    public SightingFilters limit(int limit) { this.limit = limit; return this; }

    /** Materializes the builder into a {@code Map} of query-param name → value strings. */
    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (status != null) map.put("status", status);
        if (serverIp != null) map.put("serverIp", serverIp);
        if (playerMin != null) map.put("playerMin", String.valueOf(playerMin));
        if (playerMax != null) map.put("playerMax", String.valueOf(playerMax));
        if (sort != null) map.put("sort", sort);
        if (order != null) map.put("order", order);
        if (limit != null) map.put("limit", String.valueOf(limit));
        return map;
    }
}
