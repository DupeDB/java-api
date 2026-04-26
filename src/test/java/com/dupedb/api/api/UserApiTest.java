package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserApi path construction and method delegation.
 * Verifies all 8 user endpoints (me, display-name, privacy, my-exploits,
 * my-comments, delete-exploit, connected-apps, revoke-app) route to the
 * correct paths with correct HTTP methods.
 */
class UserApiTest {

    private RecordingExecutor recorder;
    private UserApi api;

    @BeforeEach
    void setUp() {
        recorder = new RecordingExecutor();
        api = new UserApi(recorder);
    }

    // --- me ---

    @Test
    void me_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.me();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/auth/me", call.path());
    }

    // --- updateDisplayName ---

    @Test
    void updateDisplayName_delegatesToPutWithCorrectPath() throws DupeDBException {
        api.updateDisplayName("NewName");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("PUT", call.method());
        assertEquals("/api/auth/display-name", call.path());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateDisplayName_passesNameInBody() throws DupeDBException {
        api.updateDisplayName("TestUser");

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals("TestUser", body.get("displayName"));
    }

    // --- updatePrivacy ---

    @Test
    void updatePrivacy_delegatesToPutWithCorrectPath() throws DupeDBException {
        api.updatePrivacy(true);

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("PUT", call.method());
        assertEquals("/api/auth/privacy-settings", call.path());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updatePrivacy_passesHideDiscordProfileField() throws DupeDBException {
        api.updatePrivacy(true);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        Map<String, Object> body = (Map<String, Object>) call.body();
        assertEquals(true, body.get("hideDiscordProfile"));
    }

    // --- myExploits ---

    @Test
    void myExploits_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.myExploits();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/auth/my-exploits", call.path());
    }

    // --- myComments ---

    @Test
    void myComments_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.myComments();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/auth/my-comments", call.path());
    }

    // --- mySightings ---

    @Test
    void mySightings_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.mySightings();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/auth/my-sightings", call.path());
    }

    // --- deleteExploit ---

    @Test
    void deleteExploit_delegatesToDeleteWithIdInPath() throws DupeDBException {
        api.deleteExploit("exploit-123");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("DELETE", call.method());
        assertEquals("/api/auth/my-exploits/exploit-123", call.path());
    }

    // --- connectedApps ---

    @Test
    void connectedApps_delegatesToGetWithOAuthPath() throws DupeDBException {
        api.connectedApps();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/oauth/connected", call.path());
    }

    // --- revokeApp ---

    @Test
    void revokeApp_delegatesToDeleteWithAppIdInPath() throws DupeDBException {
        api.revokeApp("app-xyz");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("DELETE", call.method());
        assertEquals("/api/oauth/connected/app-xyz", call.path());
    }

    // --- deleteAccount ---

    @Test
    void deleteAccount_delegatesToDeleteWithCorrectPath() throws DupeDBException {
        api.deleteAccount();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("DELETE", call.method());
        assertEquals("/api/auth/account", call.path());
    }
}
