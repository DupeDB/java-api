package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.Comment;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** API client for exploit comment endpoints ({@code /api/exploits/:id/comments}). */
public class CommentApi {
    private final HttpExecutor http;

    public CommentApi(HttpExecutor http) {
        this.http = http;
    }

    /**
     * Lists comments on an exploit. Calls {@code GET /api/exploits/:id/comments}.
     *
     * <p>Top-level comments are returned in DESC order (newest first); each carries
     * a {@code replies} list (also DESC). Server-side reply depth is capped at 3.
     */
    public List<Comment> list(String exploitId) throws DupeDBException {
        Type type = new TypeToken<List<Comment>>() {}.getType();
        return http.get("/api/exploits/" + exploitId + "/comments", type);
    }

    /** Adds a comment to an exploit. Calls {@code POST /api/exploits/:id/comments}. */
    public Comment add(String exploitId, String content) throws DupeDBException {
        return add(exploitId, content, null, false, null);
    }

    /**
     * Adds a comment to an exploit with optional reply / sighting metadata.
     * Calls {@code POST /api/exploits/:id/comments}.
     *
     * @param exploitId       target exploit id
     * @param content         comment text (required for normal comments; optional when {@code isSighting} is true)
     * @param parentCommentId set to make this a reply (null for top-level)
     * @param isSighting      true for a server-sighting comment; requires a non-blank {@code serverIp}
     * @param serverIp        server IP for the sighting (required when {@code isSighting} is true)
     * @throws IllegalArgumentException when {@code isSighting} is true but {@code serverIp} is null/blank
     */
    public Comment add(String exploitId, String content, Integer parentCommentId,
                       boolean isSighting, String serverIp) throws DupeDBException {
        if (isSighting && (serverIp == null || serverIp.isBlank())) {
            throw new IllegalArgumentException("serverIp is required when isSighting is true");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", content);
        if (parentCommentId != null) {
            body.put("parent_comment_id", parentCommentId);
        }
        if (isSighting) {
            body.put("is_sighting", true);
            body.put("server_ip", serverIp);
        }
        AddResponse response = http.post("/api/exploits/" + exploitId + "/comments",
            body, AddResponse.class);
        return response != null ? response.comment() : null;
    }

    /** Server response wrapper for {@code POST /api/exploits/:id/comments}. */
    private record AddResponse(String message, Comment comment) {}
}
