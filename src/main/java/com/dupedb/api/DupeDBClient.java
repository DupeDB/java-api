package com.dupedb.api;

import com.dupedb.api.api.CommentApi;
import com.dupedb.api.api.CommunityApi;
import com.dupedb.api.api.DraftApi;
import com.dupedb.api.api.ExploitApi;
import com.dupedb.api.api.MediaApi;
import com.dupedb.api.api.MetadataApi;
import com.dupedb.api.api.MyAppsApi;
import com.dupedb.api.api.OAuthApi;
import com.dupedb.api.api.ResourceApi;
import com.dupedb.api.api.SightingApi;
import com.dupedb.api.api.UserApi;
import com.dupedb.api.api.UserProfileApi;
import com.dupedb.api.api.VoteApi;
import com.dupedb.api.auth.AuthManager;
import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;

import java.util.function.Supplier;

/**
 * Main client for the DupeDB API. Construct via {@link DupeDB#client()}.
 * OAuth tokens are resolved lazily per-request, not at build time.
 */
public class DupeDBClient implements AutoCloseable {
    private final String baseUrl;
    private final AuthManager authManager; // nullable for unauthenticated
    private final HttpExecutor http;
    private final ExploitApi exploitApi;
    private final VoteApi voteApi;
    private final CommentApi commentApi;
    private final DraftApi draftApi;
    private final UserApi userApi;
    private final MetadataApi metadataApi;
    private final UserProfileApi userProfileApi;
    private final CommunityApi communityApi;
    private final MediaApi mediaApi;
    private final ResourceApi resourceApi;
    private final MyAppsApi myAppsApi;
    private final OAuthApi oauthApi;
    private final SightingApi sightingApi;

    DupeDBClient(String baseUrl, AuthManager authManager) {
        this.baseUrl = baseUrl;
        this.authManager = authManager;

        Supplier<String> tokenSupplier = authManager != null
            ? () -> {
                try {
                    return authManager.getToken();
                } catch (DupeDBException e) {
                    throw new RuntimeException(e);
                }
            }
            : null;

        this.http = new HttpExecutor(baseUrl, tokenSupplier);
        this.exploitApi = new ExploitApi(http);
        this.voteApi = new VoteApi(http);
        this.commentApi = new CommentApi(http);
        this.draftApi = new DraftApi(http);
        this.userApi = new UserApi(http);
        this.metadataApi = new MetadataApi(http);
        this.userProfileApi = new UserProfileApi(http);
        this.communityApi = new CommunityApi(http);
        this.mediaApi = new MediaApi(http);
        this.resourceApi = new ResourceApi(http);
        this.myAppsApi = new MyAppsApi(http);
        this.oauthApi = new OAuthApi(http);
        this.sightingApi = new SightingApi(http);
    }

    /** Search, retrieve, and update exploits. Some endpoints require authentication. */
    public ExploitApi exploits() { return exploitApi; }

    /** Get or cast votes on exploits. Requires authentication. */
    public VoteApi votes() { return voteApi; }

    /** List and post comments on exploits. Requires authentication. */
    public CommentApi comments() { return commentApi; }

    /** Create, update, delete, and submit draft exploits. Requires authentication. */
    public DraftApi drafts() { return draftApi; }

    /** Manage the authenticated user's profile, exploits, comments, and connected apps. */
    public UserApi user() { return userApi; }

    /**
     * Access site metadata: health, version, tags, stats, and server information.
     * Most methods (tags, versions, types, stats, server IPs, plugins, latest activity) now require
     * authentication; only {@code health}, {@code version}, {@code publicStats},
     * {@code publicStatsHistory}, {@code publicExploits}, and {@code siteVisibility} are public.
     */
    public MetadataApi metadata() { return metadataApi; }

    /** Look up other users' public profiles by ID or username. */
    public UserProfileApi users() { return userProfileApi; }

    /** List and detect exploit communities. */
    public CommunityApi communities() { return communityApi; }

    /** Upload and delete profile pictures and exploit media. Requires authentication. */
    public MediaApi media() { return mediaApi; }

    /** Browse resource categories and individual resources. */
    public ResourceApi resources() { return resourceApi; }

    /**
     * Manage the authenticated user's owned OAuth applications (self-service).
     * Backed by the {@code /api/oauth/my-apps} surface introduced in Phase 105.
     * Requires authentication.
     */
    public MyAppsApi myApps() { return myAppsApi; }

    /** OAuth 2.1 protocol operations (RFC 7009 token revocation, etc.). */
    public OAuthApi oauth() { return oauthApi; }

    /**
     * Browse community-reported server sightings across all exploits via
     * {@code /api/sightings/search}. For the authenticated user's own
     * sightings see {@link #user()}.{@code mySightings()}.
     */
    public SightingApi sightings() { return sightingApi; }

    /** Returns whether a token is available without triggering an auth flow. */
    public boolean isAuthenticated() {
        return authManager != null && authManager.hasToken();
    }

    /** Clears the auth token. Next authenticated call will re-authenticate. */
    public void clearAuth() {
        if (authManager != null) {
            authManager.clearToken();
        }
    }

    /** Closes the underlying HTTP client, releasing pooled connections. */
    @Override
    public void close() {
        http.close();
    }
}
