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

    // --- edit ---

    @Test
    void edit_putsToCorrectPath() throws DupeDBException {
        api.edit(42, "updated text");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("PUT", call.method());
        assertEquals("/api/auth/my-comments/42", call.path());
    }

    @Test
    @SuppressWarnings("unchecked")
    void edit_sendsContentBody() throws DupeDBException {
        api.edit(42, "updated text");

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals("updated text", body.get("content"));
        assertEquals(1, body.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void edit_withServerIp_sendsContentAndServerIp() throws DupeDBException {
        api.edit(42, "updated text", "play.example.net");

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("PUT", call.method());
        assertEquals("/api/auth/my-comments/42", call.path());
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals("updated text", body.get("content"));
        assertEquals("play.example.net", body.get("server_ip"));
        assertEquals(2, body.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void edit_serverIpOnly_omitsContent() throws DupeDBException {
        api.edit(42, null, "play.example.net");

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertFalse(body.containsKey("content"));
        assertEquals("play.example.net", body.get("server_ip"));
        assertEquals(1, body.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void edit_contentOnly_omitsServerIp() throws DupeDBException {
        api.edit(42, "updated text", null);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals("updated text", body.get("content"));
        assertFalse(body.containsKey("server_ip"));
        assertEquals(1, body.size());
    }

    @Test
    void edit_bothNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> api.edit(42, null, null));
        assertTrue(recorder.getCalls().isEmpty());
    }

    // --- deleteOwn ---

    @Test
    void deleteOwn_deletesAtCorrectPath() throws DupeDBException {
        api.deleteOwn(99);

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("DELETE", call.method());
        assertEquals("/api/auth/my-comments/99", call.path());
    }
}
