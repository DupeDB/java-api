package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.Vote;
import com.dupedb.api.model.VoteResult;

import java.util.Map;

/** API client for exploit vote endpoints ({@code /api/exploits/:id/vote}). */
public class VoteApi {
    private final HttpExecutor http;

    public VoteApi(HttpExecutor http) {
        this.http = http;
    }

    /** Gets the current user's vote on an exploit. Calls {@code GET /api/exploits/:id/vote}. */
    public Vote get(String exploitId) throws DupeDBException {
        return http.get("/api/exploits/" + exploitId + "/vote", Vote.class);
    }

    /**
     * Casts a vote on an exploit. Calls {@code POST /api/exploits/:id/vote}.
     * @param type {@code "up"}, {@code "down"}, or {@code "none"} (the latter clears the existing vote)
     */
    public VoteResult vote(String exploitId, String type) throws DupeDBException {
        return http.post("/api/exploits/" + exploitId + "/vote", Map.of("vote", type), VoteResult.class);
    }

    /** Clears the current user's vote on an exploit. Convenience for {@code vote(exploitId, "none")}. */
    public VoteResult clear(String exploitId) throws DupeDBException {
        return vote(exploitId, "none");
    }
}
