package com.dupedb.api.model;

/**
 * A community-reported server sighting row from the cross-exploit browse list
 * returned by {@code GET /api/sightings/search}.
 *
 * <p>Different shape from {@link Sighting} (which is the per-user view from
 * {@code GET /api/auth/my-sightings}): this record carries the parent exploit
 * name and the reporter's identity so a UI can render an attribution line
 * without a second lookup. The browse path only ever returns
 * {@code is_verified = 1} rows; pending sightings are moderated separately.
 *
 * <p>{@code isPatched} is {@code 0} (still working) or {@code 1} (server patched).
 *
 * <p>{@code playerGateQualifies} disambiguates a {@code null verifiedPlayerCount}:
 * {@code null} = grandfathered (verified before the gate existed),
 * {@code true}  = ping reached the server but Bedrock or count is intentionally
 * {@code null}, {@code false} = ping failed (render as "Offline" tag).
 */
public record BrowseSighting(
    int id,
    String exploitId,
    Integer commentId,
    String serverIp,
    String serverIcon,
    Integer verifiedPlayerCount,
    Boolean playerGateQualifies,
    int isPatched,
    String createdAt,
    Integer reportedByUserId,
    String exploitName,
    String reporterUsername,
    String reporterDisplayName,
    String reporterRole
) {}
