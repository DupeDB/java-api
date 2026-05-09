package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.model.ExploitCardBatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MetadataApi path construction and method delegation.
 */
class MetadataApiTest {

    private RecordingExecutor recorder;
    private MetadataApi api;

    @BeforeEach
    void setUp() {
        recorder = new RecordingExecutor();
        api = new MetadataApi(recorder);
    }

    @Test
    void health_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.health();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/health", call.path());
    }

    @Test
    void version_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.version();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/version", call.path());
    }

    @Test
    void tags_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.tags();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/tags", call.path());
    }

    @Test
    void versions_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.versions();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/versions", call.path());
    }

    @Test
    void types_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.types();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/types", call.path());
    }

    @Test
    void serverTypes_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.serverTypes();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/server-types", call.path());
    }

    @Test
    void stats_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.stats();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/stats", call.path());
    }

    @Test
    void latestActivity_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.latestActivity();

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/latest-activity", call.path());
    }

    @Test
    void serverIps_delegatesToGetWithPaginationParams() throws DupeDBException {
        api.serverIps(2, 100);
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/server-ips?page=2&limit=100", call.path());
    }

    @Test
    void serverIps_defaultsToPage1Limit50() throws DupeDBException {
        api.serverIps();
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/server-ips?page=1&limit=50", call.path());
    }

    @Test
    void plugins_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.plugins();
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/plugins", call.path());
    }

    @Test
    void publicStats_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.publicStats();
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/public/stats", call.path());
    }

    @Test
    void publicStatsHistory_delegatesToGetWithDaysParam() throws DupeDBException {
        api.publicStatsHistory(7);
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/public/stats/history?days=7", call.path());
    }

    @Test
    void publicStatsHistory_defaultsTo30Days() throws DupeDBException {
        api.publicStatsHistory();
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/public/stats/history?days=30", call.path());
    }

    @Test
    void siteVisibility_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.siteVisibility();
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/site-visibility", call.path());
    }

    @Test
    void publicExploits_delegatesToGetWithCorrectPath() throws DupeDBException {
        api.publicExploits();
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_TYPE", call.method());
        assertEquals("/api/public/exploits", call.path());
    }

    @Test
    void newPublished_callsCorrectPath() throws DupeDBException {
        api.newPublished("2026-05-01T00:00:00Z");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/new-published?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    @Test
    void newPublished_encodesSpecialCharsInSince() throws DupeDBException {
        api.newPublished("2026-05-01T12:30:00.123+05:00");
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        // colons → %3A, plus → %2B, period stays
        assertEquals("/api/new-published?since=2026-05-01T12%3A30%3A00.123%2B05%3A00", call.path());
    }

    @Test
    void newPublished_instantOverloadDelegatesToStringForm() throws DupeDBException {
        api.newPublished(Instant.parse("2026-05-01T00:00:00Z"));
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/new-published?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    // --- exploitCardBatch ---

    @Test
    void exploitCardBatch_buildsCsvIdsParam() throws DupeDBException {
        api.exploitCardBatch(List.of("abcdef12345", "ghijkl67890"));

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/exploits-card-batch?ids=abcdef12345,ghijkl67890", call.path());
    }

    @Test
    void exploitCardBatch_emptyListShortCircuitsWithoutNetwork() throws DupeDBException {
        ExploitCardBatchResult result = api.exploitCardBatch(List.of());

        assertNotNull(result);
        assertEquals(0, result.count());
        assertTrue(result.exploits().isEmpty());
        assertTrue(recorder.getCalls().isEmpty(), "empty list must not hit network");
    }

    @Test
    void exploitCardBatch_nullListShortCircuitsWithoutNetwork() throws DupeDBException {
        ExploitCardBatchResult result = api.exploitCardBatch(null);

        assertNotNull(result);
        assertEquals(0, result.count());
        assertTrue(recorder.getCalls().isEmpty());
    }

    @Test
    void exploitCardBatch_encodesIdsWithSpecialChars() throws DupeDBException {
        api.exploitCardBatch(List.of("a/b", "c d"));
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        // / → %2F, space → +
        assertEquals("/api/exploits-card-batch?ids=a%2Fb,c+d", call.path());
    }

    // --- newUnverified ---

    @Test
    void newUnverified_callsCorrectPath() throws DupeDBException {
        api.newUnverified("2026-05-01T00:00:00Z");
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/new-unverified?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    @Test
    void newUnverified_instantOverloadDelegatesToStringForm() throws DupeDBException {
        api.newUnverified(Instant.parse("2026-05-01T00:00:00Z"));
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/new-unverified?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    // --- newUnverifiedSightings ---

    @Test
    void newUnverifiedSightings_callsCorrectPath() throws DupeDBException {
        api.newUnverifiedSightings("2026-05-01T00:00:00Z");
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/new-unverified-sightings?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    @Test
    void newUnverifiedSightings_instantOverload() throws DupeDBException {
        api.newUnverifiedSightings(Instant.parse("2026-05-01T00:00:00Z"));
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/new-unverified-sightings?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    // --- newVerifiedSightings ---

    @Test
    void newVerifiedSightings_callsCorrectPath() throws DupeDBException {
        api.newVerifiedSightings("2026-05-01T00:00:00Z");
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/new-verified-sightings?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    @Test
    void newVerifiedSightings_instantOverload() throws DupeDBException {
        api.newVerifiedSightings(Instant.parse("2026-05-01T00:00:00Z"));
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/new-verified-sightings?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    // --- newReports ---

    @Test
    void newReports_callsCorrectPath() throws DupeDBException {
        api.newReports("2026-05-01T00:00:00Z");
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/new-reports?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    @Test
    void newReports_instantOverload() throws DupeDBException {
        api.newReports(Instant.parse("2026-05-01T00:00:00Z"));
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/new-reports?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    // --- newAuditLogs ---

    @Test
    void newAuditLogs_callsCorrectPath() throws DupeDBException {
        api.newAuditLogs("2026-05-01T00:00:00Z");
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/new-audit-logs?since=2026-05-01T00%3A00%3A00Z", call.path());
    }

    @Test
    void newAuditLogs_instantOverload() throws DupeDBException {
        api.newAuditLogs(Instant.parse("2026-05-01T00:00:00Z"));
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/new-audit-logs?since=2026-05-01T00%3A00%3A00Z", call.path());
    }
}
