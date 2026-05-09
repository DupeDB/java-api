package com.dupedb.api.model;

import com.dupedb.api.internal.JsonHelper;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that all response model records correctly deserialize from snake_case JSON
 * via Gson's LOWER_CASE_WITH_UNDERSCORES naming policy.
 */
class ModelDeserializationTest {

    // ---- Exploit ----

    @Test
    void exploitDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "id": "abc123",
                "name": "Test Exploit",
                "description": "A test exploit description",
                "type": "exploit",
                "status": "verified",
                "upvotes": 10,
                "downvotes": 2,
                "views": 150,
                "date_submitted": "2026-01-01T00:00:00Z",
                "date_modified": "2026-01-02T00:00:00Z",
                "published_at": "2026-01-01T00:01:00Z",
                "last_activity_at": "2026-01-03T00:00:00Z",
                "marked_working_at": "2026-01-02T00:00:00Z",
                "marked_patched_at": null,
                "edition": "java",
                "platform": "vanilla",
                "multiplayer_type": "version",
                "minecraft_versions": ["1.21", "1.20.6"],
                "server_ips": ["mc.example.com"],
                "sources": ["https://example.com"],
                "plugin_server_ips": [],
                "mod_links": [],
                "server_software": ["paper"],
                "media_files": [],
                "embedded_videos": ["https://youtube.com/watch?v=test"],
                "plugins": ["EssentialsX"],
                "thumbnail": "/uploads/thumb.jpg",
                "redirect_url": null,
                "accent_color": "#ff5500",
                "notify_discord": true,
                "verified_player_count": 25,
                "player_gate_qualifies": true,
                "plugin_name": null,
                "plugin_version": null,
                "author": "TestUser",
                "author_user_id": 42,
                "author_display_name": "Test User",
                "author_discord_id": "987654321",
                "author_discord_avatar": "abcd1234",
                "author_custom_avatar": null,
                "author_role": "user",
                "tags": ["dupe", "item"],
                "sighted_ips": [],
                "attachedMedia": [],
                "is_draft": false,
                "rejection_reason": null,
                "verified_by": "AdminUser"
            }
            """;

        Exploit exploit = JsonHelper.fromJson(json, Exploit.class);

        assertEquals("abc123", exploit.id());
        assertEquals("Test Exploit", exploit.name());
        assertEquals("A test exploit description", exploit.description());
        assertEquals("exploit", exploit.type());
        assertEquals("verified", exploit.status());
        assertEquals(10, exploit.upvotes());
        assertEquals(2, exploit.downvotes());
        assertEquals(150, exploit.views());
        assertEquals("2026-01-01T00:00:00Z", exploit.dateSubmitted());
        assertEquals("2026-01-02T00:00:00Z", exploit.dateModified());
        assertEquals("2026-01-01T00:01:00Z", exploit.publishedAt());
        assertEquals("2026-01-03T00:00:00Z", exploit.lastActivityAt());
        assertEquals("2026-01-02T00:00:00Z", exploit.markedWorkingAt());
        assertNull(exploit.markedPatchedAt());
        assertEquals("java", exploit.edition());
        assertEquals("vanilla", exploit.platform());
        assertEquals("version", exploit.multiplayerType());
        assertEquals(List.of("1.21", "1.20.6"), exploit.minecraftVersions());
        assertEquals(List.of("mc.example.com"), exploit.serverIps());
        assertEquals(List.of("https://example.com"), exploit.sources());
        assertEquals(List.of(), exploit.pluginServerIps());
        assertEquals(List.of(), exploit.modLinks());
        assertEquals(List.of("paper"), exploit.serverSoftware());
        assertEquals(List.of(), exploit.mediaFiles());
        assertEquals(List.of("https://youtube.com/watch?v=test"), exploit.embeddedVideos());
        assertEquals(List.of("EssentialsX"), exploit.plugins());
        assertEquals("/uploads/thumb.jpg", exploit.thumbnail());
        assertNull(exploit.redirectUrl());
        assertEquals("#ff5500", exploit.accentColor());
        assertTrue(exploit.notifyDiscord());
        assertEquals(25, exploit.verifiedPlayerCount());
        assertTrue(exploit.playerGateQualifies());
        assertNull(exploit.pluginName());
        assertNull(exploit.pluginVersion());
        assertEquals("TestUser", exploit.author());
        assertEquals(42, exploit.authorUserId());
        assertEquals("Test User", exploit.authorDisplayName());
        assertEquals("987654321", exploit.authorDiscordId());
        assertEquals("abcd1234", exploit.authorDiscordAvatar());
        assertNull(exploit.authorCustomAvatar());
        assertEquals("user", exploit.authorRole());
        assertEquals(List.of("dupe", "item"), exploit.tags());
        assertEquals(List.of(), exploit.sightedIps());
        assertEquals(List.of(), exploit.attachedMedia());
        assertFalse(exploit.isDraft());
        assertNull(exploit.rejectionReason());
        assertEquals("AdminUser", exploit.verifiedBy());
    }

    @Test
    void exploitDeserializesEmbeddedComments() {
        String json = """
            {
                "id": "abc",
                "name": "With Comments",
                "comments": [
                    {
                        "id": 1,
                        "exploit_id": "abc",
                        "author": "u1",
                        "author_user_id": 10,
                        "content": "looks legit",
                        "date_posted": "2026-04-01T00:00:00Z"
                    },
                    {
                        "id": 2,
                        "exploit_id": "abc",
                        "author": "u2",
                        "author_user_id": 11,
                        "content": "confirmed",
                        "date_posted": "2026-04-01T00:01:00Z",
                        "edited_at": "2026-04-02T10:00:00Z"
                    }
                ]
            }
            """;

        Exploit exploit = JsonHelper.fromJson(json, Exploit.class);
        assertEquals(2, exploit.comments().size());
        assertEquals("looks legit", exploit.comments().get(0).content());
        assertEquals("2026-04-02T10:00:00Z", exploit.comments().get(1).editedAt());
    }

    @Test
    void exploitDeserializesSightedIpsAndAttachedMedia() {
        String json = """
            {
                "id": "xyz",
                "name": "Sighted",
                "sighted_ips": [
                    {"ip": "play.example.net", "commentId": 12},
                    {"ip": "mc.test.org", "commentId": 19}
                ],
                "attachedMedia": [
                    {
                        "filename": "abc.png",
                        "original_name": "screenshot.png",
                        "file_path": "/uploads/abc.png",
                        "file_size": 245000,
                        "mimetype": "image/png"
                    }
                ]
            }
            """;

        Exploit exploit = JsonHelper.fromJson(json, Exploit.class);

        assertEquals(2, exploit.sightedIps().size());
        assertEquals("play.example.net", exploit.sightedIps().get(0).ip());
        assertEquals(12, exploit.sightedIps().get(0).commentId());
        assertEquals(1, exploit.attachedMedia().size());
        AttachedMedia media = exploit.attachedMedia().getFirst();
        assertEquals("abc.png", media.filename());
        assertEquals("screenshot.png", media.originalName());
        assertEquals("/uploads/abc.png", media.filePath());
        assertEquals(245000L, media.fileSize());
        assertEquals("image/png", media.mimetype());
    }

    // ---- ExploitCard ----

    @Test
    void exploitCardDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "id": "xyz789",
                "name": "Card Exploit",
                "thumbnail": "/uploads/card.jpg",
                "type": "glitch",
                "status": "unverified",
                "upvotes": 5,
                "downvotes": 1,
                "views": 30,
                "view_count": 30,
                "date_submitted": "2026-02-01T00:00:00Z",
                "published_at": "2026-02-01T00:01:00Z",
                "last_activity_at": "2026-02-03T00:00:00Z",
                "marked_working_at": null,
                "marked_patched_at": "2026-02-02T00:00:00Z",
                "edition": "bedrock",
                "platform": "modded",
                "multiplayer_type": "plugin",
                "minecraft_versions": ["1.20"],
                "redirect_url": null,
                "accent_color": "#00ff00",
                "verified_player_count": null,
                "player_gate_qualifies": null,
                "plugin_name": "Essentials",
                "plugin_version": "2.20",
                "author": "CardUser",
                "author_user_id": 7,
                "author_display_name": "Card Display",
                "author_discord_id": null,
                "author_discord_avatar": null,
                "author_custom_avatar": null,
                "author_role": "user",
                "tags": ["pvp"]
            }
            """;

        ExploitCard card = JsonHelper.fromJson(json, ExploitCard.class);

        assertEquals("xyz789", card.id());
        assertEquals("Card Exploit", card.name());
        assertEquals("/uploads/card.jpg", card.thumbnail());
        assertEquals("glitch", card.type());
        assertEquals("unverified", card.status());
        assertEquals(5, card.upvotes());
        assertEquals(1, card.downvotes());
        assertEquals(30, card.views());
        assertEquals("2026-02-01T00:00:00Z", card.dateSubmitted());
        assertEquals("plugin", card.multiplayerType());
        assertEquals("bedrock", card.edition());
        assertEquals("modded", card.platform());
        assertEquals(List.of("1.20"), card.minecraftVersions());
        assertEquals("CardUser", card.author());
        assertEquals(7, card.authorUserId());
        assertEquals("Card Display", card.authorDisplayName());
        assertEquals(List.of("pvp"), card.tags());
        assertEquals("Essentials", card.pluginName());
        assertEquals("2.20", card.pluginVersion());
        assertEquals("#00ff00", card.accentColor());
        assertNull(card.verifiedPlayerCount());
        assertNull(card.playerGateQualifies());
    }

    @Test
    void exploitCardDeserializesViewCountAlternate() {
        // /api/public/exploits emits the count as `view_count` (SQL aliases
        // `e.views as view_count`); the alternate-name mapping should still
        // populate the `views` accessor.
        String json = """
            {
                "id": "card-vc",
                "name": "Public Card",
                "view_count": 12
            }
            """;

        ExploitCard card = JsonHelper.fromJson(json, ExploitCard.class);
        assertEquals(12, card.views());
    }

    // ---- SearchResult<T> ----

    @Test
    void searchResultDeserializesFromPaginatedJson() {
        String json = """
            {
                "exploits": [
                    {"id": "a1", "name": "Exploit A"},
                    {"id": "b2", "name": "Exploit B"}
                ],
                "pagination": {
                    "page": 1,
                    "limit": 20,
                    "total": 5,
                    "pages": 2,
                    "has_more": true
                }
            }
            """;

        Type type = new TypeToken<SearchResult<ExploitCard>>() {}.getType();
        SearchResult<ExploitCard> result = JsonHelper.fromJson(json, type);

        assertEquals(2, result.exploits().size());
        assertEquals("a1", result.exploits().get(0).id());
        assertEquals("Exploit B", result.exploits().get(1).name());
        assertEquals(5, result.pagination().total());
        assertEquals(1, result.pagination().page());
        assertEquals(2, result.pagination().pages());
        assertTrue(result.pagination().hasMore());
    }

    // ---- Comment ----

    @Test
    void commentDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "id": 101,
                "exploit_id": "abc123",
                "author": "commenter1",
                "author_user_id": 5,
                "content": "Great find!",
                "date_posted": "2026-03-01T12:00:00Z",
                "edited_at": null,
                "author_display_name": "Commenter One",
                "discord_id": "123456789",
                "discord_avatar": "abc",
                "custom_avatar": null,
                "author_role": "user",
                "parent_comment_id": null,
                "is_sighting": 0,
                "replies": [],
                "sighting_id": null,
                "sighting_server_ip": null,
                "sighting_verified": null,
                "sighting_patched": null
            }
            """;

        Comment comment = JsonHelper.fromJson(json, Comment.class);

        assertEquals(101, comment.id());
        assertEquals("abc123", comment.exploitId());
        assertEquals("commenter1", comment.author());
        assertEquals(5, comment.authorUserId());
        assertEquals("Great find!", comment.content());
        assertEquals("2026-03-01T12:00:00Z", comment.datePosted());
        assertNull(comment.editedAt());
        assertEquals("Commenter One", comment.authorDisplayName());
        assertEquals("123456789", comment.discordId());
        assertEquals("abc", comment.discordAvatar());
        assertNull(comment.customAvatar());
        assertEquals("user", comment.authorRole());
        assertNull(comment.parentCommentId());
        assertEquals(0, comment.isSighting());
        assertEquals(List.of(), comment.replies());
        assertNull(comment.sightingId());
    }

    @Test
    void commentDeserializesSightingFields() {
        String json = """
            {
                "id": 200,
                "exploit_id": "x",
                "author": "u",
                "author_user_id": 1,
                "content": "saw it",
                "date_posted": "2026-04-01T00:00:00Z",
                "is_sighting": 1,
                "sighting_id": 9,
                "sighting_server_ip": "play.example.net",
                "sighting_verified": 1,
                "sighting_patched": 0
            }
            """;

        Comment comment = JsonHelper.fromJson(json, Comment.class);
        assertEquals(1, comment.isSighting());
        assertEquals(9, comment.sightingId());
        assertEquals("play.example.net", comment.sightingServerIp());
        assertEquals(1, comment.sightingVerified());
        assertEquals(0, comment.sightingPatched());
    }

    @Test
    void commentDeserializesEditedAtTimestamp() {
        String json = """
            {
                "id": 5,
                "exploit_id": "e",
                "author": "u",
                "author_user_id": 1,
                "content": "edited",
                "date_posted": "2026-04-01T00:00:00Z",
                "edited_at": "2026-04-20T10:30:00.000Z"
            }
            """;

        Comment comment = JsonHelper.fromJson(json, Comment.class);
        assertEquals("2026-04-20T10:30:00.000Z", comment.editedAt());
    }

    @Test
    void commentDeserializesNestedReplies() {
        String json = """
            {
                "id": 1,
                "exploit_id": "e",
                "author": "u1",
                "author_user_id": 1,
                "content": "top",
                "date_posted": "2026-04-01T00:00:00Z",
                "replies": [
                    {
                        "id": 2,
                        "exploit_id": "e",
                        "author": "u2",
                        "author_user_id": 2,
                        "content": "reply",
                        "date_posted": "2026-04-01T00:01:00Z",
                        "parent_comment_id": 1,
                        "replies": []
                    }
                ]
            }
            """;

        Comment comment = JsonHelper.fromJson(json, Comment.class);
        assertEquals(1, comment.replies().size());
        Comment reply = comment.replies().getFirst();
        assertEquals(2, reply.id());
        assertEquals(1, reply.parentCommentId());
        assertEquals("reply", reply.content());
    }

    // ---- Vote ----

    @Test
    void voteDeserializesUpVote() {
        String json = """
            {"userVote": "up"}
            """;

        Vote vote = JsonHelper.fromJson(json, Vote.class);
        assertEquals("up", vote.userVote());
    }

    @Test
    void voteDeserializesNullUserVote() {
        String json = """
            {"userVote": null}
            """;

        Vote vote = JsonHelper.fromJson(json, Vote.class);
        assertNull(vote.userVote());
    }

    // ---- VoteResult ----

    @Test
    void voteResultDeserializesFromJson() {
        String json = """
            {
                "upvotes": 15,
                "downvotes": 3,
                "userVote": "down"
            }
            """;

        VoteResult result = JsonHelper.fromJson(json, VoteResult.class);

        assertEquals(15, result.upvotes());
        assertEquals(3, result.downvotes());
        assertEquals("down", result.userVote());
    }

    @Test
    void voteResultWithNullUserVote() {
        String json = """
            {
                "upvotes": 0,
                "downvotes": 0,
                "userVote": null
            }
            """;

        VoteResult result = JsonHelper.fromJson(json, VoteResult.class);
        assertEquals(0, result.upvotes());
        assertEquals(0, result.downvotes());
        assertNull(result.userVote());
    }

    // ---- User ----

    @Test
    void userDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "id": 1,
                "username": "testuser",
                "display_name": "Test User",
                "discord_id": "987654321",
                "discord_username": "testuser#0001",
                "discord_avatar": "avatar_hash",
                "custom_avatar": "/uploads/custom.jpg",
                "role": "admin",
                "created_at": "2026-01-01T00:00:00Z",
                "hide_discord_profile": false
            }
            """;

        User user = JsonHelper.fromJson(json, User.class);

        assertEquals(1, user.id());
        assertEquals("testuser", user.username());
        assertEquals("Test User", user.displayName());
        assertEquals("987654321", user.discordId());
        assertEquals("testuser#0001", user.discordUsername());
        assertEquals("avatar_hash", user.discordAvatar());
        assertEquals("/uploads/custom.jpg", user.customAvatar());
        assertEquals("admin", user.role());
        assertEquals("2026-01-01T00:00:00Z", user.createdAt());
        assertFalse(user.hideDiscordProfile());
    }

    // ---- Draft ----

    @Test
    void draftDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "id": "draft001",
                "name": "My Draft",
                "description": "Draft description",
                "type": "bug",
                "status": "unverified",
                "edition": "both",
                "platform": "plugin",
                "multiplayer_type": "plugin",
                "minecraft_versions": ["1.21"],
                "server_ips": [],
                "sources": [],
                "plugins": ["Factions"],
                "plugin_name": "Factions",
                "plugin_version": "3.0",
                "server_software": ["spigot"],
                "mod_links": [],
                "embedded_videos": [],
                "thumbnail": null,
                "redirect_url": null,
                "accent_color": null,
                "notify_discord": null,
                "tags": ["economy"],
                "is_draft": true
            }
            """;

        Draft draft = JsonHelper.fromJson(json, Draft.class);

        assertEquals("draft001", draft.id());
        assertEquals("My Draft", draft.name());
        assertEquals("Draft description", draft.description());
        assertEquals("bug", draft.type());
        assertEquals("unverified", draft.status());
        assertEquals("both", draft.edition());
        assertEquals("plugin", draft.platform());
        assertEquals("plugin", draft.multiplayerType());
        assertEquals(List.of("1.21"), draft.minecraftVersions());
        assertEquals(List.of(), draft.serverIps());
        assertEquals(List.of(), draft.sources());
        assertEquals(List.of("Factions"), draft.plugins());
        assertEquals("Factions", draft.pluginName());
        assertEquals("3.0", draft.pluginVersion());
        assertEquals(List.of("spigot"), draft.serverSoftware());
        assertEquals(List.of(), draft.modLinks());
        assertEquals(List.of(), draft.embeddedVideos());
        assertNull(draft.thumbnail());
        assertNull(draft.redirectUrl());
        assertNull(draft.accentColor());
        assertNull(draft.notifyDiscord());
        assertEquals(List.of("economy"), draft.tags());
        assertTrue(draft.isDraft());
    }

    // ---- Pagination ----

    @Test
    void paginationDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "page": 2,
                "pages": 5,
                "total": 48,
                "has_more": true
            }
            """;

        Pagination pagination = JsonHelper.fromJson(json, Pagination.class);

        assertEquals(2, pagination.page());
        assertEquals(5, pagination.pages());
        assertEquals(48, pagination.total());
        assertTrue(pagination.hasMore());
    }

    // ---- ConnectedApp ----

    @Test
    void connectedAppDeserializesFromCamelCaseServerJson() {
        // ConnectedApp.id is the refresh-token family UUID per Phase 103 cutover
        // (commit a9f52bb) — String, not int.
        String json = """
            {
                "id": "fam-42",
                "appId": "my-app",
                "appName": "My Cool App",
                "readOnly": true,
                "createdAt": "2026-03-01T00:00:00Z",
                "lastUsedAt": "2026-03-15T12:00:00Z"
            }
            """;

        ConnectedApp app = JsonHelper.fromJson(json, ConnectedApp.class);

        assertEquals("fam-42", app.id());
        assertEquals("my-app", app.appId());
        assertEquals("My Cool App", app.appName());
        assertTrue(app.readOnly());
        assertEquals("2026-03-01T00:00:00Z", app.createdAt());
        assertEquals("2026-03-15T12:00:00Z", app.lastUsedAt());
    }

    // ---- Null handling ----

    @Test
    void nullJsonFieldsMapToNullRecordComponents() {
        String json = """
            {
                "id": "nulltest",
                "name": "Null Test",
                "description": null,
                "type": "exploit",
                "status": "unverified",
                "upvotes": 0,
                "downvotes": 0,
                "views": 0,
                "date_submitted": null,
                "edition": null,
                "platform": null,
                "multiplayer_type": null,
                "minecraft_versions": null,
                "server_ips": null,
                "sources": null,
                "plugin_server_ips": null,
                "mod_links": null,
                "server_software": null,
                "media_files": null,
                "embedded_videos": null,
                "plugins": null,
                "thumbnail": null,
                "author": null,
                "author_user_id": null,
                "author_display_name": null,
                "tags": null,
                "plugin_name": null,
                "plugin_version": null,
                "is_draft": false,
                "rejection_reason": null,
                "accent_color": null,
                "verified_by": null
            }
            """;

        Exploit exploit = JsonHelper.fromJson(json, Exploit.class);

        assertEquals("nulltest", exploit.id());
        assertEquals("Null Test", exploit.name());
        assertNull(exploit.description());
        assertNull(exploit.dateSubmitted());
        assertNull(exploit.minecraftVersions());
        assertNull(exploit.serverIps());
        assertNull(exploit.thumbnail());
        assertNull(exploit.authorUserId());
        assertNull(exploit.authorDisplayName());
        assertNull(exploit.tags());
        assertNull(exploit.rejectionReason());
        assertNull(exploit.accentColor());
        assertNull(exploit.verifiedBy());
        assertNull(exploit.multiplayerType());
    }

    @Test
    void connectedAppNullFieldsHandled() {
        String json = """
            {
                "id": 1,
                "appId": "test",
                "appName": "Test",
                "readOnly": false,
                "createdAt": null,
                "lastUsedAt": null
            }
            """;

        ConnectedApp app = JsonHelper.fromJson(json, ConnectedApp.class);
        assertNull(app.createdAt());
        assertNull(app.lastUsedAt());
    }

    // ---- Tag ----

    @Test
    void tagDeserializesFromJson() {
        String json = """
            {"id": 1, "name": "dupe", "count": 42}
            """;

        Tag tag = JsonHelper.fromJson(json, Tag.class);
        assertEquals(1, tag.id());
        assertEquals("dupe", tag.name());
        assertEquals(42, tag.count());
    }

    // ---- SiteStats ----

    @Test
    void siteStatsDeserializesFromCamelCaseJson() {
        String json = """
            {
                "total": 150,
                "byType": [
                    {"type": "dupe", "count": 80},
                    {"type": "glitch", "count": 70}
                ],
                "byStatus": [
                    {"status": "verified", "count": 100},
                    {"status": "unverified", "count": 50}
                ],
                "recentCount": 12,
                "verifiedCount": 100,
                "unverifiedCount": 50,
                "userCount": 500
            }
            """;

        SiteStats stats = JsonHelper.fromJson(json, SiteStats.class);
        assertEquals(150, stats.total());
        assertEquals(2, stats.byType().size());
        assertEquals("dupe", stats.byType().get(0).type());
        assertEquals(80, stats.byType().get(0).count());
        assertEquals(2, stats.byStatus().size());
        assertEquals("verified", stats.byStatus().get(0).status());
        assertEquals(12, stats.recentCount());
        assertEquals(100, stats.verifiedCount());
        assertEquals(50, stats.unverifiedCount());
        assertEquals(500, stats.userCount());
    }

    // ---- LatestActivity ----

    @Test
    void latestActivityDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "id": 99,
                "name": "Test Exploit",
                "status": "verified",
                "date_submitted": "2026-04-01T00:00:00Z",
                "date_modified": "2026-04-02T00:00:00Z",
                "author": "testuser",
                "author_user_id": 5,
                "author_display_name": "Test User",
                "author_custom_avatar": "/uploads/avatar.png",
                "author_discord_id": "123456",
                "author_discord_avatar": "abc123"
            }
            """;

        LatestActivity activity = JsonHelper.fromJson(json, LatestActivity.class);
        assertEquals(99, activity.id());
        assertEquals("Test Exploit", activity.name());
        assertEquals("verified", activity.status());
        assertEquals("2026-04-01T00:00:00Z", activity.dateSubmitted());
        assertEquals("2026-04-02T00:00:00Z", activity.dateModified());
        assertEquals("testuser", activity.author());
        assertEquals(5, activity.authorUserId());
        assertEquals("Test User", activity.authorDisplayName());
        assertEquals("/uploads/avatar.png", activity.authorCustomAvatar());
        assertEquals("123456", activity.authorDiscordId());
        assertEquals("abc123", activity.authorDiscordAvatar());
    }

    // ---- UserProfile ----

    @Test
    void userProfileDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "id": 42,
                "username": "testuser",
                "display_name": "Test User",
                "role": "moderator",
                "custom_avatar": "/uploads/avatar.png",
                "created_at": "2025-01-01T00:00:00Z",
                "exploit_count": 15,
                "total_upvotes": 200,
                "total_views": 1234,
                "discord_id": "123456",
                "discord_avatar": "abc",
                "is_banned": false,
                "timeout_until": null,
                "is_system_account": false,
                "learn_more_url": null
            }
            """;

        UserProfile profile = JsonHelper.fromJson(json, UserProfile.class);
        assertEquals(42, profile.id());
        assertEquals("testuser", profile.username());
        assertEquals("Test User", profile.displayName());
        assertEquals("moderator", profile.role());
        assertEquals("/uploads/avatar.png", profile.customAvatar());
        assertEquals("2025-01-01T00:00:00Z", profile.createdAt());
        assertEquals(15, profile.exploitCount());
        assertEquals(200, profile.totalUpvotes());
        assertEquals(1234, profile.totalViews());
        assertEquals("123456", profile.discordId());
        assertEquals("abc", profile.discordAvatar());
        assertFalse(profile.isBanned());
        assertNull(profile.timeoutUntil());
        assertFalse(profile.isSystemAccount());
        assertNull(profile.learnMoreUrl());
    }

    // ---- UserLookup ----

    @Test
    void userLookupDeserializesFromSnakeCaseJson() {
        String json = """
            {
                "id": 7,
                "username": "lookupuser",
                "display_name": "Lookup User",
                "role": "user",
                "custom_avatar": null,
                "discord_id": "999",
                "discord_avatar": "xyz"
            }
            """;

        UserLookup lookup = JsonHelper.fromJson(json, UserLookup.class);
        assertEquals(7, lookup.id());
        assertEquals("lookupuser", lookup.username());
        assertEquals("Lookup User", lookup.displayName());
        assertEquals("user", lookup.role());
        assertNull(lookup.customAvatar());
        assertEquals("999", lookup.discordId());
        assertEquals("xyz", lookup.discordAvatar());
    }

    // ---- Community ----

    @Test
    void communityDeserializesFromCamelCaseJson() {
        String json = """
            {
                "id": 3,
                "platform": "discord",
                "platformId": "123456789",
                "platformDisplayName": "Discord",
                "actionText": "Join",
                "url": "https://discord.gg/test",
                "name": "Test Community",
                "description": "A test community",
                "customDescription": "Custom desc",
                "fetchedDescription": "Fetched desc",
                "displayOrder": 1,
                "isVisible": true,
                "createdAt": "2025-06-01T00:00:00Z",
                "updatedAt": "2025-06-15T00:00:00Z",
                "createdByUsername": "admin",
                "iconUrl": "https://cdn.example.com/icon.png",
                "bannerUrl": null,
                "memberCount": 1500,
                "extraData": null,
                "fetchedAt": "2025-06-14T00:00:00Z",
                "fetchError": null,
                "isCacheFresh": true,
                "accentColor": "#5865F2"
            }
            """;

        Community community = JsonHelper.fromJson(json, Community.class);
        assertEquals(3, community.id());
        assertEquals("discord", community.platform());
        assertEquals("123456789", community.platformId());
        assertEquals("Discord", community.platformDisplayName());
        assertEquals("Join", community.actionText());
        assertEquals("https://discord.gg/test", community.url());
        assertEquals("Test Community", community.name());
        assertEquals("A test community", community.description());
        assertEquals("Custom desc", community.customDescription());
        assertEquals("Fetched desc", community.fetchedDescription());
        assertEquals(1, community.displayOrder());
        assertTrue(community.isVisible());
        assertEquals("2025-06-01T00:00:00Z", community.createdAt());
        assertEquals("2025-06-15T00:00:00Z", community.updatedAt());
        assertEquals("admin", community.createdByUsername());
        assertEquals("https://cdn.example.com/icon.png", community.iconUrl());
        assertNull(community.bannerUrl());
        assertEquals(1500, community.memberCount());
        assertNull(community.extraData());
        assertEquals("2025-06-14T00:00:00Z", community.fetchedAt());
        assertNull(community.fetchError());
        assertTrue(community.isCacheFresh());
        assertEquals("#5865F2", community.accentColor());
    }

    // ---- MediaUpload ----

    @Test
    void mediaUploadDeserializesFromMixedCaseJson() {
        String json = """
            {
                "success": true,
                "url": "/uploads/screenshot.png",
                "filename": "screenshot-abc123.png",
                "size": 204800,
                "type": "image",
                "mimetype": "image/png",
                "thumbnailUrl": null,
                "draftId": "draft-1",
                "exploitId": "draft-1",
                "resourceId": null,
                "uploadType": "exploit"
            }
            """;

        MediaUpload upload = JsonHelper.fromJson(json, MediaUpload.class);
        assertTrue(upload.success());
        assertEquals("/uploads/screenshot.png", upload.url());
        assertEquals("screenshot-abc123.png", upload.filename());
        assertEquals(204800, upload.size());
        assertEquals("image", upload.type());
        assertEquals("image/png", upload.mimetype());
        assertNull(upload.thumbnailUrl());
        assertEquals("draft-1", upload.draftId());
        assertEquals("draft-1", upload.exploitId());
        assertNull(upload.resourceId());
        assertEquals("exploit", upload.uploadType());
    }

    // ---- ProfilePictureResult ----

    @Test
    void profilePictureResultDeserializesFromJson() {
        String json = """
            {
                "success": true,
                "message": "Profile picture updated successfully",
                "avatarUrl": "/uploads/avatar-xyz.png"
            }
            """;

        ProfilePictureResult result = JsonHelper.fromJson(json, ProfilePictureResult.class);
        assertTrue(result.success());
        assertEquals("Profile picture updated successfully", result.message());
        assertEquals("/uploads/avatar-xyz.png", result.avatarUrl());
    }

    // ---- DeleteMediaResult ----

    @Test
    void deleteMediaResultDeserializesFromJson() {
        String json = """
            {
                "success": true,
                "freedSize": 512000,
                "message": "File deleted successfully"
            }
            """;

        DeleteMediaResult result = JsonHelper.fromJson(json, DeleteMediaResult.class);
        assertTrue(result.success());
        assertEquals(512000, result.freedSize());
        assertEquals("File deleted successfully", result.message());
    }

    // ---- ResourceCategory ----

    @Test
    void resourceCategoryDeserializesFromSnakeCaseJson() {
        String json = """
            {"id": 1, "name": "Guides", "display_order": 2, "created_at": "2025-01-01T00:00:00Z", "updated_at": "2025-06-01T00:00:00Z"}
            """;
        ResourceCategory cat = JsonHelper.fromJson(json, ResourceCategory.class);
        assertEquals(1, cat.id());
        assertEquals("Guides", cat.name());
        assertEquals(2, cat.displayOrder());
    }

    // ---- Resource ----

    @Test
    void resourceDeserializesFromCamelCaseJson() {
        String json = """
            {
                "id": 5, "title": "Getting Started", "slug": "getting-started",
                "description": "A guide", "content": "<p>Hello</p>",
                "categoryId": 1, "categoryName": "Guides",
                "resourceType": "guide", "externalUrl": null,
                "iconUrl": "/uploads/icon.png", "bannerUrl": null,
                "isPublished": true, "createdBy": 42,
                "authorUsername": "admin", "authorDisplayName": "Admin",
                "authorDiscordId": null, "authorDiscordAvatar": null,
                "authorCustomAvatar": null, "authorRole": "admin",
                "createdAt": "2025-01-01T00:00:00Z", "updatedAt": "2025-06-01T00:00:00Z"
            }
            """;
        Resource r = JsonHelper.fromJson(json, Resource.class);
        assertEquals(5, r.id());
        assertEquals("Getting Started", r.title());
        assertEquals("getting-started", r.slug());
        assertEquals(1, r.categoryId());
        assertEquals("Guides", r.categoryName());
        assertEquals("guide", r.resourceType());
        assertTrue(r.isPublished());
        assertEquals("admin", r.authorUsername());
    }

    // ---- Plugin ----

    @Test
    void pluginDeserializesFromJson() {
        String json = """
            {"name": "EssentialsX", "versions": ["2.20.1", "2.19.0"]}
            """;
        Plugin p = JsonHelper.fromJson(json, Plugin.class);
        assertEquals("EssentialsX", p.name());
        assertEquals(List.of("2.20.1", "2.19.0"), p.versions());
    }

    // ---- PublicStats ----

    @Test
    void publicStatsDeserializesFromCamelCaseJson() {
        String json = """
            {"verifiedCount": 100, "unverifiedCount": 50, "userCount": 500}
            """;
        PublicStats stats = JsonHelper.fromJson(json, PublicStats.class);
        assertEquals(100, stats.verifiedCount());
        assertEquals(50, stats.unverifiedCount());
        assertEquals(500, stats.userCount());
    }

    // ---- StatsSnapshot ----

    @Test
    void statsSnapshotDeserializesFromJson() {
        String json = """
            {"timestamp": "2025-06-01T00:00:00Z", "verified": 95, "users": 480}
            """;
        StatsSnapshot s = JsonHelper.fromJson(json, StatsSnapshot.class);
        assertEquals("2025-06-01T00:00:00Z", s.timestamp());
        assertEquals(95, s.verified());
        assertEquals(480, s.users());
    }

    // ---- HealthStatus ----

    @Test
    void healthStatusDeserializesFromJson() {
        String json = """
            {"status": "healthy", "timestamp": "2026-04-01T00:00:00Z", "error": null}
            """;

        HealthStatus health = JsonHelper.fromJson(json, HealthStatus.class);
        assertEquals("healthy", health.status());
        assertEquals("2026-04-01T00:00:00Z", health.timestamp());
        assertNull(health.error());
    }

    @Test
    void healthStatusDeserializesUnhealthyState() {
        String json = """
            {"status": "unhealthy", "timestamp": "2026-04-01T00:00:00Z", "error": "Database connection failed"}
            """;

        HealthStatus health = JsonHelper.fromJson(json, HealthStatus.class);
        assertEquals("unhealthy", health.status());
        assertEquals("Database connection failed", health.error());
    }

    // ---- VersionInfo ----

    @Test
    void versionInfoDeserializesFromCamelCaseJson() {
        String json = """
            {
                "commitCount": 542,
                "lastCommitMessage": "fix: update auth flow",
                "lastCommitDate": "2026-04-01",
                "authorName": "developer",
                "githubUsername": "developer-gh"
            }
            """;

        VersionInfo info = JsonHelper.fromJson(json, VersionInfo.class);
        assertEquals(542, info.commitCount());
        assertEquals("fix: update auth flow", info.lastCommitMessage());
        assertEquals("2026-04-01", info.lastCommitDate());
        assertEquals("developer", info.authorName());
        assertEquals("developer-gh", info.githubUsername());
    }

    // ---- CommunityDetection ----

    @Test
    void communityDetectionDeserializesDetectedResult() {
        String json = """
            {
                "detected": true,
                "platform": "discord",
                "platformId": "123456789",
                "apiConfigured": true,
                "apiRequired": false
            }
            """;

        CommunityDetection result = JsonHelper.fromJson(json, CommunityDetection.class);
        assertTrue(result.detected());
        assertEquals("discord", result.platform());
        assertEquals("123456789", result.platformId());
        assertTrue(result.apiConfigured());
        assertFalse(result.apiRequired());
        assertNull(result.hint());
    }

    @Test
    void communityDetectionDeserializesNotDetectedResult() {
        String json = """
            {
                "detected": false,
                "hint": "Supported: discord.gg, youtube.com/@, reddit.com/r/, twitter.com, x.com"
            }
            """;

        CommunityDetection result = JsonHelper.fromJson(json, CommunityDetection.class);
        assertFalse(result.detected());
        assertNull(result.platform());
        assertNull(result.platformId());
        assertNotNull(result.hint());
        assertTrue(result.hint().contains("discord.gg"));
    }

    // ---- SiteVisibility ----

    @Test
    void siteVisibilityDeserializesFromSnakeCaseJson() {
        String json = """
            {"scanner_hidden": true, "community_hidden": false}
            """;
        SiteVisibility v = JsonHelper.fromJson(json, SiteVisibility.class);
        assertTrue(v.scannerHidden());
        assertFalse(v.communityHidden());
    }

    // ---- ServerIpResult ----

    @Test
    void serverIpResultDeserializesFromJson() {
        String json = """
            {
                "serverIps": ["mc.example.com", "play.test.net"],
                "pagination": {"page": 1, "pages": 3, "total": 120, "has_more": true}
            }
            """;
        ServerIpResult r = JsonHelper.fromJson(json, ServerIpResult.class);
        assertEquals(List.of("mc.example.com", "play.test.net"), r.serverIps());
        assertEquals(1, r.pagination().page());
        assertEquals(120, r.pagination().total());
        assertTrue(r.pagination().hasMore());
    }
}
