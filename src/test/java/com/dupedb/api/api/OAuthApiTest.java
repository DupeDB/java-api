package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OAuthApi (RFC 7009 token revocation). Verifies the form-encoded
 * POST is dispatched to the correct path with the correct field shape.
 */
class OAuthApiTest {

    private RecordingExecutor recorder;
    private OAuthApi api;

    @BeforeEach
    void setUp() {
        recorder = new RecordingExecutor();
        api = new OAuthApi(recorder);
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_delegatesToPostFormWithCorrectPath() throws DupeDBException {
        api.revoke("dupe_abc123", "refresh_token");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("POST_FORM", call.method());
        assertEquals("/api/oauth/revoke", call.path());

        Map<String, String> body = (Map<String, String>) call.body();
        assertEquals("dupe_abc123", body.get("token"));
        assertEquals("refresh_token", body.get("token_type_hint"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_omitsHintWhenNull() throws DupeDBException {
        api.revoke("dupe_abc123", null);

        Map<String, String> body = (Map<String, String>) recorder.getCalls().getFirst().body();
        assertEquals("dupe_abc123", body.get("token"));
        assertFalse(body.containsKey("token_type_hint"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_includesAccessTokenHint() throws DupeDBException {
        api.revoke("opaque_access", "access_token");

        Map<String, String> body = (Map<String, String>) recorder.getCalls().getFirst().body();
        assertEquals("access_token", body.get("token_type_hint"));
    }
}
