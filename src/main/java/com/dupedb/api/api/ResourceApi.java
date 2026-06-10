package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.Resource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * API client for resource endpoints ({@code /api/resources}).
 *
 * <p>Categories are a fixed slug set ({@code "documentation"}, {@code "video"},
 * {@code "mods"}) since June 2026 — the old {@code GET /api/resources/categories}
 * endpoint and its numeric category IDs were removed. {@link #categories()} now
 * reads the slug list off the list response.
 */
public class ResourceApi {
    private final HttpExecutor http;

    public ResourceApi(HttpExecutor http) {
        this.http = http;
    }

    /** Combined list response: published resources + the fixed category slugs. */
    private record ResourcesResponse(List<Resource> resources, List<String> categories) {}

    /** Single-resource response envelope shared by {@link #getById} and {@link #getBySlug}. */
    private record ResourceResponse(Resource resource) {}

    /**
     * Lists the resource category slugs. Calls {@code GET /api/resources/}
     * and returns the {@code categories} array from the response. Note this
     * transfers the full published-resources list too — if you need both,
     * call {@link #list()} once and read categories from your own copy.
     */
    public List<String> categories() throws DupeDBException {
        ResourcesResponse response = http.get("/api/resources/", ResourcesResponse.class);
        return response != null && response.categories() != null ? response.categories() : List.of();
    }

    /** Lists all published resources. Calls {@code GET /api/resources/}. */
    public List<Resource> list() throws DupeDBException {
        ResourcesResponse response = http.get("/api/resources/", ResourcesResponse.class);
        return response != null ? response.resources() : List.of();
    }

    /**
     * Lists published resources filtered by category slug
     * ({@code "documentation"}, {@code "video"}, or {@code "mods"}).
     * Calls {@code GET /api/resources/?category=:slug}. Unknown slugs are
     * ignored server-side and return the unfiltered list.
     */
    public List<Resource> list(String category) throws DupeDBException {
        ResourcesResponse response = http.get("/api/resources/?category=" + encode(category), ResourcesResponse.class);
        return response != null ? response.resources() : List.of();
    }

    /** Gets a resource by ID. Calls {@code GET /api/resources/id/:id}. */
    public Resource getById(int id) throws DupeDBException {
        ResourceResponse response = http.get("/api/resources/id/" + id, ResourceResponse.class);
        return response != null ? response.resource() : null;
    }

    /** Gets a resource by URL slug. Calls {@code GET /api/resources/:slug}. */
    public Resource getBySlug(String slug) throws DupeDBException {
        ResourceResponse response = http.get("/api/resources/" + encode(slug), ResourceResponse.class);
        return response != null ? response.resource() : null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
