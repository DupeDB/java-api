package com.dupedb.api.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CredentialsTest {

    @Test
    void recordHoldsAllFiveFields() {
        Instant exp = Instant.parse("2026-05-01T13:00:00Z");
        Credentials c = new Credentials(
            "access_xyz",
            "my-app",
            "2026-05-01T12:00:00Z",
            "refresh_abc",
            exp
        );
        assertEquals("access_xyz", c.accessToken());
        assertEquals("my-app", c.appId());
        assertEquals("2026-05-01T12:00:00Z", c.createdAt());
        assertEquals("refresh_abc", c.refreshToken());
        assertEquals(exp, c.expiresAt());
    }

    @Test
    void recordIsImmutable() {
        // Records are immutable by definition — this test documents the contract
        Credentials c = new Credentials("a", "b", "c", "d", Instant.EPOCH);
        assertEquals("a", c.accessToken());
        // No setters; only accessor methods. If a future refactor adds a
        // mutator the compiler will reject it (records cannot have setters).
    }
}
