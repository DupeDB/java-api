package com.dupedb.api.model;

/**
 * Reason supplied to {@code POST /api/exploits/:id/report}.
 *
 * <p>{@link #PATCHED} reports do not require a description; all other types
 * require a non-blank {@code message}.
 */
public enum ReportType {
    PATCHED("patched"),
    CUSTOM("custom"),
    DISCLOSURE("disclosure"),
    INACCURATE("inaccurate"),
    MISCREDIT("miscredit");

    private final String wire;

    ReportType(String wire) {
        this.wire = wire;
    }

    /** Returns the wire value sent to the server (e.g. {@code "patched"}). */
    public String wire() {
        return wire;
    }

    /** Whether the server requires a non-blank {@code message} when this report type is submitted. */
    public boolean requiresMessage() {
        return this != PATCHED;
    }
}
