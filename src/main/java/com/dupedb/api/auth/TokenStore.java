package com.dupedb.api.auth;

import com.dupedb.api.internal.JsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persists and loads auth credentials from {@code ~/.dupedb/token.json}. */
public class TokenStore {
    private final Path filePath;

    /** Creates a TokenStore using the default path. */
    public TokenStore() {
        this(Path.of(System.getProperty("user.home"), ".dupedb", "token.json"));
    }

    /** Creates a TokenStore using a custom file path. */
    public TokenStore(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Loads credentials, or null if missing/corrupted/legacy.
     *
     * <p>Per Phase 103 D-16, v1.x format files (with the legacy {@code token}
     * field, lacking {@code accessToken}/{@code refreshToken}/{@code expiresAt})
     * are silently invalidated — Gson populates the missing fields as null,
     * and any null in the new shape returns null from this method, triggering
     * a fresh OAuth flow on the next {@link AuthManager#getToken()} call.</p>
     */
    public Credentials load() {
        if (!Files.exists(filePath)) {
            return null;
        }
        try {
            String json = Files.readString(filePath);
            Credentials c = JsonHelper.fromJson(json, Credentials.class);
            // 1.x files lack accessToken/refreshToken/expiresAt — Gson populates
            // missing fields as null. Treat any null as invalidation → caller
            // (AuthManager) triggers a fresh OAuth flow per D-16.
            if (c == null
                    || c.accessToken() == null
                    || c.refreshToken() == null
                    || c.expiresAt() == null) {
                return null;
            }
            return c;
        } catch (IOException e) {
            return null; // Corrupted file, treat as no token
        }
    }

    /** Saves credentials to disk. Creates parent directories if needed. */
    public void save(Credentials credentials) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, JsonHelper.toJson(credentials));
    }

    /** Deletes the token file (best-effort). */
    public void delete() {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // Best effort cleanup
        }
    }

    public Path getFilePath() { return filePath; }
}
