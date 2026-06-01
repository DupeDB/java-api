package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DraftApi path construction and method delegation.
 * Verifies all 5 draft endpoints route to the correct paths with correct
 * HTTP methods and that getCurrent() makes exactly one HTTP call.
 */
class DraftApiTest {

    private RecordingExecutor recorder;
    private DraftApi api;

    @BeforeEach
    void setUp() {
        recorder = new RecordingExecutor();
        api = new DraftApi(recorder);
    }

    // --- getCurrent ---

    @Test
    void getCurrent_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.getCurrent();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/exploits/draft", call.path());
    }

    @Test
    void getCurrent_makesExactlyOneHttpCall() throws DupeDBException {
        api.getCurrent();

        // Critical: must be exactly 1 call, not 2 (no dead double-call bug)
        assertEquals(1, recorder.getCalls().size());
    }

    // --- create ---

    @Test
    void create_postsThenRefetchesDraft() throws DupeDBException {
        Map<String, Object> data = Map.of("name", "New Exploit");
        api.create(data);

        // POST creates the draft, then a GET re-fetches the saved state — the
        // create endpoint itself only returns {id, message}.
        assertEquals(2, recorder.getCalls().size());
        RecordingExecutor.Call post = recorder.getCalls().get(0);
        assertEquals("POST", post.method());
        assertEquals("/api/exploits/draft", post.path());
        assertSame(data, post.body());

        RecordingExecutor.Call refetch = recorder.getCalls().get(1);
        assertEquals("GET_CLASS", refetch.method());
        assertEquals("/api/exploits/draft", refetch.path());
    }

    // --- update ---

    @Test
    void update_putsThenRefetchesDraft() throws DupeDBException {
        Map<String, Object> data = Map.of("name", "Updated");
        api.update("draft-456", data);

        // PUT saves the edit, then a GET re-fetches the saved state — the
        // update endpoint itself only returns {message}.
        assertEquals(2, recorder.getCalls().size());
        RecordingExecutor.Call put = recorder.getCalls().get(0);
        assertEquals("PUT", put.method());
        assertEquals("/api/exploits/draft/draft-456", put.path());
        assertSame(data, put.body());

        RecordingExecutor.Call refetch = recorder.getCalls().get(1);
        assertEquals("GET_CLASS", refetch.method());
        assertEquals("/api/exploits/draft", refetch.path());
    }

    // --- delete ---

    @Test
    void delete_callsDeleteWithCorrectPath() throws DupeDBException {
        api.delete("draft-789");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("DELETE", call.method());
        assertEquals("/api/exploits/draft/draft-789", call.path());
    }

    // --- submit ---

    @Test
    void submit_callsPostWithSubmitSuffix() throws DupeDBException {
        api.submit("draft-abc");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("POST", call.method());
        assertEquals("/api/exploits/draft/draft-abc/submit", call.path());
    }
}
