package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.Draft;

import java.util.Map;

/** API client for draft exploit endpoints ({@code /api/exploits/draft}). */
public class DraftApi {
    private final HttpExecutor http;

    public DraftApi(HttpExecutor http) {
        this.http = http;
    }

    /** Gets the current user's draft, or null if none exists. Calls {@code GET /api/exploits/draft}. */
    public Draft getCurrent() throws DupeDBException {
        DraftResponse response = http.get("/api/exploits/draft", DraftResponse.class);
        return response != null ? response.draft() : null;
    }

    /**
     * Creates a new draft and returns the saved state. Only one draft per account.
     * Calls {@code POST /api/exploits/draft}, then re-fetches via {@link #getCurrent()}
     * (the create endpoint itself only returns {@code {id, message}}).
     */
    public Draft create(Map<String, Object> data) throws DupeDBException {
        http.post("/api/exploits/draft", data, Void.class);
        return getCurrent();
    }

    /**
     * Updates a draft and returns the saved state. Calls {@code PUT /api/exploits/draft/:id},
     * then re-fetches via {@link #getCurrent()} (the update endpoint itself only returns
     * {@code {message}}). Callers driving a tight auto-save loop may ignore the return value.
     */
    public Draft update(String id, Map<String, Object> data) throws DupeDBException {
        http.put("/api/exploits/draft/" + id, data, Void.class);
        return getCurrent();
    }

    /** Deletes a draft. Calls {@code DELETE /api/exploits/draft/:id}. */
    public void delete(String id) throws DupeDBException {
        http.delete("/api/exploits/draft/" + id);
    }

    /** Submits a draft for review. Calls {@code POST /api/exploits/draft/:id/submit}. */
    public void submit(String id) throws DupeDBException {
        http.post("/api/exploits/draft/" + id + "/submit", Map.of(), Void.class);
    }

    /** Wrapper for the server's nested {@code {"draft": {...}}} response. */
    private record DraftResponse(Draft draft) {}
}
