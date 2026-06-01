package com.dupedb.api.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Type-safe builder for {@link ExploitApi#search} filter parameters.
 *
 * <p>All setters return {@code this} for chaining. Call {@link #toMap()} to
 * materialize a query-string-ready {@code Map}, or pass the builder directly to
 * {@link ExploitApi#search(String, int, SearchFilters)}.
 *
 * <p>Booleans serialize as {@code "true"} / {@code "false"} (the server accepts
 * both that and {@code "1"} / {@code "0"}).
 */
public final class SearchFilters {
    private String type;
    private String status;
    private String edition;
    private String platform;
    private String version;
    private String software;
    private String serverIp;
    private String plugin;
    private String tag;
    private String sort;
    private String order;
    private Integer limit;
    private Boolean sightingEligibleOnly;
    private String excludeId;
    private Boolean autocomplete;

    /** Filter by exploit type (comma-separated for multi-select). */
    public SearchFilters type(String type) { this.type = type; return this; }

    /** Filter by status: {@code verified}, {@code unverified}, {@code working}, {@code patched} (comma-separated). */
    public SearchFilters status(String status) { this.status = status; return this; }

    /** Filter by Minecraft edition: {@code java}, {@code bedrock} (comma-separated). */
    public SearchFilters edition(String edition) { this.edition = edition; return this; }

    /** Filter by platform: {@code singleplayer}, {@code multiplayer} (comma-separated). */
    public SearchFilters platform(String platform) { this.platform = platform; return this; }

    /** Filter by Minecraft version (e.g. {@code "1.21.4"}, comma-separated). */
    public SearchFilters version(String version) { this.version = version; return this; }

    /** Filter by server software (comma-separated). */
    public SearchFilters software(String software) { this.software = software; return this; }

    /** Filter by server IP. */
    public SearchFilters serverIp(String serverIp) { this.serverIp = serverIp; return this; }

    /** Filter by plugin name. */
    public SearchFilters plugin(String plugin) { this.plugin = plugin; return this; }

    /** Filter by tag name. */
    public SearchFilters tag(String tag) { this.tag = tag; return this; }

    /** Sort field: {@code date_submitted}, {@code date_discovered}, {@code name}, {@code upvotes}, {@code views}, {@code type}, {@code closest_match}. */
    public SearchFilters sort(String sort) { this.sort = sort; return this; }

    /** Sort order: {@code asc} or {@code desc}. */
    public SearchFilters order(String order) { this.order = order; return this; }

    /** Results per page (1-100). */
    public SearchFilters limit(int limit) { this.limit = limit; return this; }

    /** Restrict results to plugin/version exploits (those eligible for community sightings). */
    public SearchFilters sightingEligibleOnly(boolean value) { this.sightingEligibleOnly = value; return this; }

    /** Exclude a specific exploit id from results. */
    public SearchFilters excludeId(String id) { this.excludeId = id; return this; }

    /** Compact title-only matching for typeahead UIs. Disables pagination. */
    public SearchFilters autocomplete(boolean value) { this.autocomplete = value; return this; }

    /** Materializes the builder into a {@code Map} of query-param name → value strings. */
    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (type != null) map.put("type", type);
        if (status != null) map.put("status", status);
        if (edition != null) map.put("edition", edition);
        if (platform != null) map.put("platform", platform);
        if (version != null) map.put("version", version);
        if (software != null) map.put("software", software);
        if (serverIp != null) map.put("serverIp", serverIp);
        if (plugin != null) map.put("plugin", plugin);
        if (tag != null) map.put("tag", tag);
        if (sort != null) map.put("sort", sort);
        if (order != null) map.put("order", order);
        if (limit != null) map.put("limit", String.valueOf(limit));
        if (sightingEligibleOnly != null) map.put("sightingEligibleOnly", sightingEligibleOnly ? "true" : "false");
        if (excludeId != null) map.put("excludeId", excludeId);
        if (autocomplete != null) map.put("autocomplete", autocomplete ? "true" : "false");
        return map;
    }
}
