package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommentApiTest {

    private RecordingExecutor recorder;
    private CommentApi api;

    @BeforeEach
    void setUp() {
        recorder = new RecordingExecutor();
        api = new CommentApi(recorder);
    }

    // --- list ---

    @Test
    void list_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.list("exploit-123");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/exploits/exploit-123/comments", call.path());
    }

    // --- add ---

    @Test
    void add_delegatesToPostWithCorrectPath() throws DupeDBException {
        api.add("exploit-123", "Great find!");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("POST", call.method());
        assertEquals("/api/exploits/exploit-123/comments", call.path());
    }

    @Test
    @SuppressWarnings("unchecked")
    void add_passesContentInBody() throws DupeDBException {
        api.add("exploit-123", "Great find!");

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals("Great find!", body.get("content"));
        assertFalse(body.containsKey("parent_comment_id"));
        assertFalse(body.containsKey("is_sighting"));
        assertFalse(body.containsKey("server_ip"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void add_withParent_includesParentCommentId() throws DupeDBException {
        api.add("exploit-123", "thanks!", 42, false, null);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals("thanks!", body.get("content"));
        assertEquals(42, body.get("parent_comment_id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void add_withSighting_includesSightingFlagAndServerIp() throws DupeDBException {
        api.add("exploit-123", "saw it on this server", null, true, "play.example.net");

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals(true, body.get("is_sighting"));
        assertEquals("play.example.net", body.get("server_ip"));
    }

    @Test
    void add_sightingWithoutServerIp_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> api.add("exploit-123", "x", null, true, null));
        assertThrows(IllegalArgumentException.class,
            () -> api.add("exploit-123", "x", null, true, "   "));
    }
}
