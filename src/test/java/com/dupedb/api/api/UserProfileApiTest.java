package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserProfileApi path construction and method delegation.
 */
class UserProfileApiTest {

    private RecordingExecutor recorder;
    private UserProfileApi api;

    @BeforeEach
    void setUp() {
        recorder = new RecordingExecutor();
        api = new UserProfileApi(recorder);
    }

    @Test
    void getProfile_delegatesToGetWithUserIdInPath() throws DupeDBException {
        api.getProfile(42);

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/users/42/profile", call.path());
    }

    @Test
    void lookup_delegatesToGetWithNameInPath() throws DupeDBException {
        api.lookup("testuser");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/users/lookup/testuser", call.path());
    }

    @Test
    void lookup_urlEncodesNameWithSpaces() throws DupeDBException {
        api.lookup("user name");

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/users/lookup/user+name", call.path());
    }

    // --- getProfileByDiscordId ---

    @Test
    void getProfileByDiscordId_buildsDiscordIdPath() throws DupeDBException {
        api.getProfileByDiscordId("123456789012345678");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/users/discord/123456789012345678/profile", call.path());
    }

    // --- getContributionsByDiscordId ---

    @Test
    void getContributionsByDiscordId_buildsPathWithLimitAndOffset() throws DupeDBException {
        api.getContributionsByDiscordId("123456789012345678", 25, 50);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals(
            "/api/users/discord/123456789012345678/contributions?limit=25&offset=50",
            call.path()
        );
    }

    @Test
    void getContributionsByDiscordId_defaultsToLimit10Offset0() throws DupeDBException {
        api.getContributionsByDiscordId("123456789012345678");

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals(
            "/api/users/discord/123456789012345678/contributions?limit=10&offset=0",
            call.path()
        );
    }

    // --- discordPoints ---

    @Test
    void discordPoints_callsCorrectPath() throws DupeDBException {
        api.discordPoints();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/users/discord-points", call.path());
    }

    // --- search ---

    @Test
    void search_buildsPathWithEncodedQuery() throws DupeDBException {
        api.search("alice");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/users/search?q=alice", call.path());
    }

    @Test
    void search_encodesSpecialChars() throws DupeDBException {
        api.search("user name");
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/users/search?q=user+name", call.path());
    }

    @Test
    void search_nullQueryEncodesEmpty() throws DupeDBException {
        api.search(null);
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/users/search?q=", call.path());
    }
}
