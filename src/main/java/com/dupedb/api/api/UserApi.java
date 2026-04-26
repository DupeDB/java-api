package com.dupedb.api.api;

import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.internal.HttpExecutor;
import com.dupedb.api.model.Comment;
import com.dupedb.api.model.ConnectedApp;
import com.dupedb.api.model.Exploit;
import com.dupedb.api.model.Sighting;
import com.dupedb.api.model.User;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/** API client for current user profile and connected apps ({@code /api/auth}, {@code /api/oauth/connected}). */
public class UserApi {
    private final HttpExecutor http;

    public UserApi(HttpExecutor http) {
        this.http = http;
    }

    /** Gets the current user's profile. Calls {@code GET /api/auth/me}. */
    public User me() throws DupeDBException {
        record MeResponse(User user) {}
        MeResponse response = http.get("/api/auth/me", MeResponse.class);
        return response != null ? response.user() : null;
    }

    /** Updates the current user's display name. Calls {@code PUT /api/auth/display-name}. */
    public void updateDisplayName(String name) throws DupeDBException {
        http.put("/api/auth/display-name", Map.of("displayName", name), Void.class);
    }

    /**
     * Updates the current user's privacy settings. Calls {@code PUT /api/auth/privacy-settings}.
     * @param hideDiscordProfile whether to hide Discord avatar and username from other users
     */
    public void updatePrivacy(boolean hideDiscordProfile) throws DupeDBException {
        http.put("/api/auth/privacy-settings", Map.of("hideDiscordProfile", hideDiscordProfile), Void.class);
    }

    /** Lists the current user's exploits. Calls {@code GET /api/auth/my-exploits}. */
    public List<Exploit> myExploits() throws DupeDBException {
        record ExploitsResponse(java.util.List<Exploit> exploits) {}
        ExploitsResponse response = http.get("/api/auth/my-exploits", ExploitsResponse.class);
        return response != null ? response.exploits() : List.of();
    }

    /** Lists the current user's comments. Calls {@code GET /api/auth/my-comments}. */
    public List<Comment> myComments() throws DupeDBException {
        Type type = new TypeToken<List<Comment>>() {}.getType();
        return http.get("/api/auth/my-comments", type);
    }

    /** Lists the current user's server sightings. Calls {@code GET /api/auth/my-sightings}. */
    public List<Sighting> mySightings() throws DupeDBException {
        Type type = new TypeToken<List<Sighting>>() {}.getType();
        return http.get("/api/auth/my-sightings", type);
    }

    /** Deletes one of the current user's exploits. Calls {@code DELETE /api/auth/my-exploits/:id}. */
    public void deleteExploit(String id) throws DupeDBException {
        http.delete("/api/auth/my-exploits/" + id);
    }

    /** Lists connected OAuth apps. Calls {@code GET /api/oauth/connected}. */
    public List<ConnectedApp> connectedApps() throws DupeDBException {
        Type type = new TypeToken<List<ConnectedApp>>() {}.getType();
        return http.get("/api/oauth/connected", type);
    }

    /** Revokes a connected OAuth app. Calls {@code DELETE /api/oauth/connected/:appId}. */
    public void revokeApp(String appId) throws DupeDBException {
        http.delete("/api/oauth/connected/" + appId);
    }

    /** Permanently deletes the current user's account. Irreversible. Calls {@code DELETE /api/auth/account}. */
    public void deleteAccount() throws DupeDBException {
        http.delete("/api/auth/account");
    }
}
