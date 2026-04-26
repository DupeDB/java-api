/**
 * Wire-format records for DupeDB API responses. All records deserialize via
 * {@link com.dupedb.api.internal.JsonHelper} using Gson's
 * {@code LOWER_CASE_WITH_UNDERSCORES} naming policy. Fields whose Java name
 * does not derive cleanly from the snake_case JSON key carry an explicit
 * {@code @SerializedName}.
 *
 * <h2>Field semantics worth knowing</h2>
 *
 * <ul>
 *   <li><b>Multiplayer type vs. plugin-specific.</b> {@link com.dupedb.api.model.Exploit#multiplayerType()},
 *       {@link com.dupedb.api.model.ExploitCard#multiplayerType()}, and
 *       {@link com.dupedb.api.model.Draft#multiplayerType()} are the authoritative
 *       multiplayer-scope flag. Values: {@code "plugin"} (works only with a specific
 *       plugin), {@code "version"} (works only on specific Minecraft / server-software
 *       versions), or {@code null} (singleplayer or scope-agnostic). The legacy
 *       {@code is_plugin_specific} field was removed in 2.0.0.</li>
 *
 *   <li><b>Vote semantics.</b> {@link com.dupedb.api.model.Vote#userVote()} and
 *       {@link com.dupedb.api.model.VoteResult#userVote()} are {@code String}: one of
 *       {@code "up"}, {@code "down"}, or {@code null} (no vote). To clear an existing
 *       vote, post {@code "none"} via {@link com.dupedb.api.api.VoteApi#vote(String, String)}
 *       or call {@link com.dupedb.api.api.VoteApi#clear(String)}.</li>
 *
 *   <li><b>Comment threading.</b> {@link com.dupedb.api.model.Comment#replies()} contains
 *       nested replies in DESC order (newest reply first). The server caps reply depth at
 *       3 levels — replies that would exceed the cap are flattened into the deepest
 *       allowed parent.</li>
 *
 *   <li><b>Sighting comments.</b> A {@link com.dupedb.api.model.Comment} with
 *       {@code isSighting() == 1} carries {@code sightingId}, {@code sightingServerIp},
 *       {@code sightingVerified} (0/1), and {@code sightingPatched} (0/1). Sightings
 *       attach to a server IP and may be verified by staff.</li>
 *
 *   <li><b>Player gate.</b> {@link com.dupedb.api.model.Exploit#playerGateQualifies()}
 *       and {@link com.dupedb.api.model.Exploit#verifiedPlayerCount()} reflect the
 *       server-side player-count check applied at sighting verification (Bedrock and
 *       grandfathered rows are exempt; for those, {@code playerGateQualifies} is
 *       {@code null}).</li>
 *
 *   <li><b>Working / patched timestamps.</b> {@code markedWorkingAt} and
 *       {@code markedPatchedAt} are mutually exclusive — setting one clears the other.
 *       Either may be null to indicate the status has never been set.</li>
 * </ul>
 */
package com.dupedb.api.model;
