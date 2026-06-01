package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SightingApi path construction. Verifies the cross-exploit
 * sighting browse endpoint encodes its query, page, and filters correctly.
 */
class SightingApiTest {

    private RecordingExecutor recorder;
    private SightingApi api;

    @BeforeEach
    void setUp() {
        recorder = new RecordingExecutor();
        api = new SightingApi(recorder);
    }

    // --- search ---

    @Test
    void search_buildsBasicPathWithoutFilters() throws DupeDBException {
        api.search("play.example", 1);

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/sightings/search?q=play.example&page=1", call.path());
    }

    @Test
    void search_emptyQueryEncodesToEmptyParam() throws DupeDBException {
        api.search("", 1);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/sightings/search?q=&page=1", call.path());
    }

    @Test
    void search_nullQueryTreatedAsEmpty() throws DupeDBException {
        api.search(null, 1);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/sightings/search?q=&page=1", call.path());
    }

    @Test
    void search_appendsFiltersAfterPage() throws DupeDBException {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("status", "working");
        filters.put("playerMin", "20");
        api.search("hub", 2, filters);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals(
            "/api/sightings/search?q=hub&page=2&status=working&playerMin=20",
            call.path()
        );
    }

    @Test
    void search_encodesFilterValuesWithSpecialChars() throws DupeDBException {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("serverIp", "play.example.net,hub.test:25565");
        api.search("", 1, filters);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        // commas and colons are reserved → percent-encoded
        assertTrue(call.path().contains("serverIp=play.example.net%2Chub.test%3A25565"),
            "Expected encoded serverIp filter, got: " + call.path());
    }

    @Test
    void search_withSightingFilters_includesEveryParam() throws DupeDBException {
        api.search("", 1, new SightingFilters()
            .status("working,patched")
            .serverIp("mc.example.com")
            .playerMin(10)
            .playerMax(200)
            .sort("verified_player_count")
            .order("desc")
            .limit(50));

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        String path = call.path();
        assertTrue(path.contains("status=working%2Cpatched"), path);
        assertTrue(path.contains("serverIp=mc.example.com"), path);
        assertTrue(path.contains("playerMin=10"), path);
        assertTrue(path.contains("playerMax=200"), path);
        assertTrue(path.contains("sort=verified_player_count"), path);
        assertTrue(path.contains("order=desc"), path);
        assertTrue(path.contains("limit=50"), path);
    }

    @Test
    void search_withNullSightingFilters_omitsFilterParams() throws DupeDBException {
        api.search("hub", 1, (SightingFilters) null);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/sightings/search?q=hub&page=1", call.path());
    }

    @Test
    void search_withNullFilterValue_skipsEntryWithoutThrowing() throws DupeDBException {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("status", "working");
        filters.put("playerMin", null); // must be skipped, not NPE on encode(null)
        api.search("", 1, filters);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertTrue(call.path().contains("status=working"));
        assertFalse(call.path().contains("playerMin"));
    }

    // --- autocomplete ---

    @Test
    void autocomplete_appendsAutocompleteFlagAndPrefix() throws DupeDBException {
        api.autocomplete("play");

        assertEquals(1, recorder.getCalls().size());
        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("GET_CLASS", call.method());
        assertEquals("/api/sightings/search?autocomplete=1&q=play", call.path());
    }

    @Test
    void autocomplete_nullPrefixTreatedAsEmpty() throws DupeDBException {
        api.autocomplete(null);

        RecordingExecutor.Call call = recorder.getCalls().getFirst();
        assertEquals("/api/sightings/search?autocomplete=1&q=", call.path());
    }
}
