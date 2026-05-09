package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.SightingSearchResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Cross-exploit sighting browse and search ({@code /api/sightings}). Different
 * surface from {@link UserApi#mySightings()} — that one returns the
 * authenticated user's own sightings; this one is the community-wide list.
 *
 * <p>The browse path returns verified rows only; pending sightings are
 * moderated through the staff queue. Authentication is required (Bearer or
 * session); there is no public/anonymous mode.
 */
public class SightingApi {
    private final HttpExecutor http;

    public SightingApi(HttpExecutor http) {
        this.http = http;
    }

    /**
     * Searches sightings across all exploits. Calls {@code GET /api/sightings/search}.
     *
     * <p>Supported filter keys (all optional): {@code sort}
     * ({@code date_submitted}, {@code verified_player_count}, {@code server_ip}),
     * {@code order} ({@code asc}/{@code desc}), {@code limit} (1-100, default 20),
     * {@code status} (CSV of {@code working}, {@code patched}),
     * {@code serverIp} (CSV of server IPs to match), {@code playerMin},
     * {@code playerMax} (player count bounds; pass empty string for no bound).
     *
     * @param query   free-text query (matches server_ip and exploit name); may be empty
     * @param page    1-based page number
     * @param filters optional filter params (may be null)
     */
    public SightingSearchResult search(String query, int page, Map<String, String> filters)
            throws DupeDBException {
        StringBuilder path = new StringBuilder("/api/sightings/search?");
        path.append("q=").append(encode(query != null ? query : ""));
        path.append("&page=").append(page);
        if (filters != null) {
            for (var entry : filters.entrySet()) {
                path.append("&").append(encode(entry.getKey()))
                    .append("=").append(encode(entry.getValue()));
            }
        }
        return http.get(path.toString(), SightingSearchResult.class);
    }

    /** Searches sightings without filters. */
    public SightingSearchResult search(String query, int page) throws DupeDBException {
        return search(query, page, null);
    }

    /**
     * Returns up to 8 distinct server IPs matching the given prefix. Calls
     * {@code GET /api/sightings/search?autocomplete=1&q=<prefix>}. Verified-only
     * and parent-exploit-visibility-gated server-side.
     */
    public List<String> autocomplete(String partialServerIp) throws DupeDBException {
        record ServerHit(String serverIp) {}
        record AutocompleteResponse(List<ServerHit> servers) {}
        String path = "/api/sightings/search?autocomplete=1&q="
            + encode(partialServerIp != null ? partialServerIp : "");
        AutocompleteResponse resp = http.get(path, AutocompleteResponse.class);
        if (resp == null || resp.servers() == null) return List.of();
        return resp.servers().stream().map(ServerHit::serverIp).toList();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
