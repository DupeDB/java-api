package com.dupedb.api.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TokenStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveWritesJsonToConfiguredPath() throws IOException {
        Path tokenFile = tempDir.resolve("token.json");
        TokenStore store = new TokenStore(tokenFile);

        store.save(new Credentials(
            "access_abc123",
            "my-app",
            "2026-04-06T12:00:00Z",
            "refresh_xyz",
            Instant.parse("2026-04-06T13:00:00Z")
        ));

        assertTrue(Files.exists(tokenFile));
        String content = Files.readString(tokenFile);
        assertTrue(content.contains("access_abc123"), "Should contain accessToken value");
        assertTrue(content.contains("my-app"), "Should contain appId value");
        assertTrue(content.contains("2026-04-06T12:00:00Z"), "Should contain createdAt value");
        assertTrue(content.contains("refresh_xyz"), "Should contain refreshToken value");
        assertTrue(content.contains("2026-04-06T13:00:00Z"), "Should contain expiresAt value");
    }

    @Test
    void loadReadsJsonFromConfiguredPath() throws IOException {
        Path tokenFile = tempDir.resolve("token.json");
        TokenStore store = new TokenStore(tokenFile);

        Instant exp = Instant.parse("2026-04-06T16:00:00Z");
        store.save(new Credentials(
            "access_xyz",
            "scanner-app",
            "2026-04-06T15:00:00Z",
            "refresh_abc",
            exp
        ));

        Credentials loaded = store.load();
        assertNotNull(loaded);
        assertEquals("access_xyz", loaded.accessToken());
        assertEquals("scanner-app", loaded.appId());
        assertEquals("2026-04-06T15:00:00Z", loaded.createdAt());
        assertEquals("refresh_abc", loaded.refreshToken());
        assertEquals(exp, loaded.expiresAt());
    }

    @Test
    void loadReturnsNullWhenFileDoesNotExist() {
        Path nonexistent = tempDir.resolve("missing/token.json");
        TokenStore store = new TokenStore(nonexistent);

        Credentials loaded = store.load();
        assertNull(loaded, "Should return null when file does not exist");
    }

    @Test
    void deleteRemovesTheFile() throws IOException {
        Path tokenFile = tempDir.resolve("token.json");
        TokenStore store = new TokenStore(tokenFile);

        store.save(new Credentials(
            "access_abc123",
            "my-app",
            "2026-04-06T12:00:00Z",
            "refresh_xyz",
            Instant.parse("2026-04-06T13:00:00Z")
        ));
        assertTrue(Files.exists(tokenFile));

        store.delete();
        assertFalse(Files.exists(tokenFile));
    }

    @Test
    void deleteDoesNotThrowWhenFileDoesNotExist() {
        Path nonexistent = tempDir.resolve("nonexistent.json");
        TokenStore store = new TokenStore(nonexistent);

        assertDoesNotThrow(store::delete);
    }

    @Test
    void saveCreatesParentDirectoriesIfNeeded() throws IOException {
        Path nested = tempDir.resolve("a/b/c/token.json");
        TokenStore store = new TokenStore(nested);

        store.save(new Credentials(
            "access_abc123",
            "my-app",
            "2026-04-06T12:00:00Z",
            "refresh_xyz",
            Instant.parse("2026-04-06T13:00:00Z")
        ));

        assertTrue(Files.exists(nested));
        Credentials loaded = store.load();
        assertNotNull(loaded);
        assertEquals("access_abc123", loaded.accessToken());
    }

    @Test
    void defaultPathUsesUserHomeDupedbTokenJson() {
        TokenStore store = new TokenStore();
        Path expected = Path.of(System.getProperty("user.home"), ".dupedb", "token.json");

        assertEquals(expected, store.getFilePath());
    }

    @Test
    void customPathOverridesDefault() {
        Path custom = Path.of("/custom/path/token.json");
        TokenStore store = new TokenStore(custom);

        assertEquals(custom, store.getFilePath());
    }

    @Test
    void load_legacy1xFile_invalidates() throws IOException {
        // Write a 1.x format JSON (only `token`/`appId`/`createdAt` fields)
        Path tokenFile = tempDir.resolve("token.json");
        Files.writeString(tokenFile,
            "{\"token\":\"dupe_legacy123\",\"app_id\":\"old-app\",\"created_at\":\"2026-03-01T00:00:00Z\"}");

        TokenStore store = new TokenStore(tokenFile);
        assertNull(store.load(),
            "1.x files (with `token` field, no `accessToken`/`refreshToken`/`expiresAt`) must invalidate to null");
    }

    @Test
    void load_partialV2File_invalidates() throws IOException {
        // accessToken present but refreshToken missing — still invalidate
        Path tokenFile = tempDir.resolve("token.json");
        Files.writeString(tokenFile,
            "{\"access_token\":\"new\",\"app_id\":\"a\",\"created_at\":\"2026-05-01T00:00:00Z\"}");

        TokenStore store = new TokenStore(tokenFile);
        assertNull(store.load(), "missing refreshToken/expiresAt invalidates");
    }
}
