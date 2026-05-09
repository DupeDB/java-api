package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MyAppsApi path construction and method delegation.
 * Verifies the four /api/oauth/my-apps endpoints route correctly and that
 * {@code update()} omits null fields from the PATCH body.
 */
class MyAppsApiTest {

    private RecordingExecutor recorder;
    private MyAppsApi api;

    @BeforeEach
    void setUp() {
        recorder = new RecordingExecutor();
        api = new MyAppsApi(recorder);
    }

    // --- list ---

    @Test
    void list_delegatesToGetClass() throws DupeDBException {
        api.list();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/oauth/my-apps", call.path());
    }

    // --- create ---

    @Test
    @SuppressWarnings("unchecked")
    void create_delegatesToPostWithFullBody() throws DupeDBException {
        api.create("my-app", "My Cool App", List.of("https://example.com/cb"), true);

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("POST", call.method());
        assertEquals("/api/oauth/my-apps", call.path());

        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals("my-app", body.get("id"));
        assertEquals("My Cool App", body.get("name"));
        assertEquals(List.of("https://example.com/cb"), body.get("redirectUris"));
        assertEquals(true, body.get("readOnly"));
    }

    // --- update ---

    @Test
    @SuppressWarnings("unchecked")
    void update_delegatesToPatchWithFilteredBody() throws DupeDBException {
        api.update("my-app", "Renamed", null, null);

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("PATCH", call.method());
        assertEquals("/api/oauth/my-apps/my-app", call.path());

        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals("Renamed", body.get("name"));
        assertFalse(body.containsKey("redirectUris"));
        assertFalse(body.containsKey("readOnly"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void update_includesAllProvidedFields() throws DupeDBException {
        api.update("my-app", "Renamed", List.of("https://new.example/cb"), false);

        Map<String, Object> body = (Map<String, Object>) recorder.getCalls().getFirst().body();
        assertEquals("Renamed", body.get("name"));
        assertEquals(List.of("https://new.example/cb"), body.get("redirectUris"));
        assertEquals(false, body.get("readOnly"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void update_omitsAllNullFields() throws DupeDBException {
        // Backend treats absent fields as no-op; an empty body is valid PATCH.
        api.update("my-app", null, null, null);

        Map<String, Object> body = (Map<String, Object>) recorder.getCalls().getFirst().body();
        assertTrue(body.isEmpty());
    }

    // --- delete ---

    @Test
    void delete_delegatesToDeleteWithIdInPath() throws DupeDBException {
        api.delete("my-app");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("DELETE", call.method());
        assertEquals("/api/oauth/my-apps/my-app", call.path());
    }
}
