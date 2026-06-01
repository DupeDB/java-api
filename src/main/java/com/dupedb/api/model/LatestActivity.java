package com.dupedb.api.model;

/**
 * The most recent activity on the site, for homepage activity display. Returned by
 * {@code GET /api/latest-activity}.
 *
 * <p>This is a discriminated union keyed on {@link #kind()}, which is either
 * {@code "exploit"} (a freshly submitted or verified exploit) or {@code "sighting"}
 * (a newly verified server sighting). Exploit-variant fields ({@link #id()},
 * {@link #name()}, the {@code author*} fields, …) are populated when
 * {@link #isExploit()} is true; sighting-variant fields ({@link #sightingId()},
 * {@link #serverIp()}, {@link #exploitName()}, the {@code reporter*} fields, …) are
 * populated when {@link #isSighting()} is true. Fields belonging to the other
 * variant are null. {@link com.dupedb.api.api.MetadataApi#latestActivity()} returns
 * null when there is no activity at all.
 */
public record LatestActivity(
    String kind,

    // --- exploit variant ---
    Integer id,
    String name,
    String status,
    String dateSubmitted,
    String publishedAt,
    String dateModified,
    String author,
    Integer authorUserId,
    String authorDisplayName,
    String authorCustomAvatar,
    String authorDiscordId,
    String authorDiscordAvatar,

    // --- sighting variant ---
    Integer sightingId,
    String exploitId,
    String serverIp,
    String verifiedAt,
    String exploitName,
    String exploitStatus,
    Integer reportedByUserId,
    String reporterDisplayName,
    String reporterCustomAvatar,
    String reporterDiscordId,
    String reporterDiscordAvatar
) {
    /** True when this activity describes an exploit submission/verification. */
    public boolean isExploit() {
        return "exploit".equals(kind);
    }

    /** True when this activity describes a verified server sighting. */
    public boolean isSighting() {
        return "sighting".equals(kind);
    }
}
