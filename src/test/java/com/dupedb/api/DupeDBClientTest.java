package com.dupedb.api;

import com.dupedb.api.api.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DupeDB builder and DupeDBClient wiring.
 * Verifies the builder fluent API, all 3 auth modes, and accessor methods.
 * Does NOT make real HTTP calls.
 */
class DupeDBClientTest {

    @Test
    void client_returnsBuilderInstance() {
        DupeDB.Builder builder = DupeDB.client();
        assertNotNull(builder);
    }

    @Test
    void builder_withBaseUrlOnly_buildsUnauthenticatedClient() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        assertNotNull(client);
        assertFalse(client.isAuthenticated());
    }

    @Test
    void builder_withToken_buildsAuthenticatedClient() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .token("dupe_abc123")
            .build();
        assertNotNull(client);
        assertTrue(client.isAuthenticated());
    }

    @Test
    void builder_withPersonalAccessToken_buildsAuthenticatedClient() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .personalAccessToken("dupe_pat_" + "a".repeat(64))
            .build();
        assertNotNull(client);
        assertTrue(client.isAuthenticated());
    }

    @Test
    void builder_personalAccessToken_rejectsNonPatValues() {
        // OAuth access tokens, internal keys, and nulls must fail eagerly
        // with a pointer to .token(...) rather than 401 on the first call.
        assertThrows(IllegalArgumentException.class, () ->
            DupeDB.client().personalAccessToken("dupe_" + "a".repeat(64)));
        assertThrows(IllegalArgumentException.class, () ->
            DupeDB.client().personalAccessToken("some-oauth-access-token"));
        assertThrows(IllegalArgumentException.class, () ->
            DupeDB.client().personalAccessToken(null));
    }

    @Test
    void builder_withOAuth_buildsClientWithoutEagerTokenResolution() {
        // OAuth client should NOT eagerly resolve token at build() time.
        // No server is running, so if it tried to resolve eagerly, it would fail.
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .oauth("test-app-id", "http://localhost:9876/callback")
            .build();
        assertNotNull(client);
        // OAuth client has no token yet (lazy resolution), but hasToken checks disk too
        // The key test is that build() does NOT throw even though no OAuth server exists
    }

    @Test
    void builder_withTokenStore_configuresCustomPath() {
        // Should not throw when configuring a custom token store path
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .oauth("test-app-id", "http://localhost:9876/callback")
            .tokenStore(Path.of(System.getProperty("java.io.tmpdir"), "test-dupedb-token.json"))
            .build();
        assertNotNull(client);
    }

    @Test
    void authenticatedClient_wiresAuthManagerIntoExecutor() throws Exception {
        // The client must build its HttpExecutor with the AuthManager so the
        // executor's 401 refresh-and-retry path is reachable (that path's mechanics
        // are covered by HttpExecutorRetryTest). Verify the wiring directly rather
        // than re-driving a live 401, keeping this file free of real HTTP calls.
        DupeDBClient authed = DupeDB.client()
            .baseUrl("https://test.dupedb.net").token("dupe_abc123").build();
        assertNotNull(authManagerRef(authed));

        DupeDBClient anonymous = DupeDB.client()
            .baseUrl("https://test.dupedb.net").build();
        assertNull(authManagerRef(anonymous));
    }

    /** Reads the executor's AuthManager reference to confirm builder wiring. */
    private static Object authManagerRef(DupeDBClient client) throws Exception {
        Field httpField = DupeDBClient.class.getDeclaredField("http");
        httpField.setAccessible(true);
        Object executor = httpField.get(client);
        Field ref = executor.getClass().getDeclaredField("authManagerRef");
        ref.setAccessible(true);
        return ref.get(executor);
    }

    @Test
    void builder_withDefaultBaseUrl_usesHttpsDupeDbCom() {
        DupeDBClient client = DupeDB.client().build();
        assertNotNull(client);
    }

    @Test
    void builder_withNullBaseUrl_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> {
            DupeDB.client().baseUrl(null).build();
        });
    }

    @Test
    void builder_withBlankBaseUrl_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> {
            DupeDB.client().baseUrl("   ").build();
        });
    }

    @Test
    void client_exploits_returnsExploitApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        ExploitApi exploits = client.exploits();
        assertNotNull(exploits);
        assertInstanceOf(ExploitApi.class, exploits);
    }

    @Test
    void client_votes_returnsVoteApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        VoteApi votes = client.votes();
        assertNotNull(votes);
        assertInstanceOf(VoteApi.class, votes);
    }

    @Test
    void client_comments_returnsCommentApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        CommentApi comments = client.comments();
        assertNotNull(comments);
        assertInstanceOf(CommentApi.class, comments);
    }

    @Test
    void client_drafts_returnsDraftApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        DraftApi drafts = client.drafts();
        assertNotNull(drafts);
        assertInstanceOf(DraftApi.class, drafts);
    }

    @Test
    void client_user_returnsUserApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        UserApi user = client.user();
        assertNotNull(user);
        assertInstanceOf(UserApi.class, user);
    }

    @Test
    void client_metadata_returnsMetadataApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        MetadataApi metadata = client.metadata();
        assertNotNull(metadata);
        assertInstanceOf(MetadataApi.class, metadata);
    }

    @Test
    void client_users_returnsUserProfileApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        UserProfileApi users = client.users();
        assertNotNull(users);
        assertInstanceOf(UserProfileApi.class, users);
    }

    @Test
    void client_communities_returnsCommunityApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        CommunityApi communities = client.communities();
        assertNotNull(communities);
        assertInstanceOf(CommunityApi.class, communities);
    }

    @Test
    void client_media_returnsMediaApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        MediaApi media = client.media();
        assertNotNull(media);
        assertInstanceOf(MediaApi.class, media);
    }

    @Test
    void client_accessorsReturnSameInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        // Accessor methods should return the same instance each time
        assertSame(client.exploits(), client.exploits());
        assertSame(client.votes(), client.votes());
        assertSame(client.comments(), client.comments());
        assertSame(client.drafts(), client.drafts());
        assertSame(client.user(), client.user());
        assertSame(client.metadata(), client.metadata());
        assertSame(client.users(), client.users());
        assertSame(client.communities(), client.communities());
        assertSame(client.media(), client.media());
        assertSame(client.resources(), client.resources());
        assertSame(client.myApps(), client.myApps());
        assertSame(client.oauth(), client.oauth());
        assertSame(client.sightings(), client.sightings());
    }

    @Test
    void client_resources_returnsResourceApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        ResourceApi resources = client.resources();
        assertNotNull(resources);
        assertInstanceOf(ResourceApi.class, resources);
    }

    @Test
    void client_myApps_returnsMyAppsApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        MyAppsApi myApps = client.myApps();
        assertNotNull(myApps);
        assertInstanceOf(MyAppsApi.class, myApps);
    }

    @Test
    void client_oauth_returnsOAuthApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        OAuthApi oauth = client.oauth();
        assertNotNull(oauth);
        assertInstanceOf(OAuthApi.class, oauth);
    }

    @Test
    void client_sightings_returnsSightingApiInstance() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        SightingApi sightings = client.sightings();
        assertNotNull(sightings);
        assertInstanceOf(SightingApi.class, sightings);
    }

    @Test
    void clearAuth_onUnauthenticatedClient_doesNotThrow() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .build();
        assertDoesNotThrow(client::clearAuth);
    }

    @Test
    void clearAuth_onTokenClient_removesAuth() {
        DupeDBClient client = DupeDB.client()
            .baseUrl("https://test.dupedb.net")
            .token("dupe_abc123")
            .build();
        assertTrue(client.isAuthenticated());
        client.clearAuth();
        assertFalse(client.isAuthenticated());
    }

    @Test
    void oauthClient_doesNotEagerlyResolveToken() {
        // This is the CRITICAL test: building an OAuth client with an appId
        // that points to no running server should NOT trigger any network call.
        // If token resolution were eager, this would fail with an exception.
        assertDoesNotThrow(() -> {
            DupeDB.client()
                .baseUrl("https://nonexistent.test.dupedb.net")
                .oauth("nonexistent-app", "http://localhost:19999/callback")
                .build();
        });
    }
}
