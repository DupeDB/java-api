package com.dupedb.api;

import com.dupedb.api.auth.AuthManager;
import com.dupedb.api.auth.OAuthFlow;
import com.dupedb.api.auth.TokenStore;

import java.nio.file.Path;

/** Static entry point for constructing a {@link DupeDBClient}. Default base URL: {@code https://dupedb.net}. */
public final class DupeDB {
    private static final String DEFAULT_BASE_URL = "https://dupedb.net";

    private DupeDB() {} // No instantiation

    /** Creates a new builder for constructing a {@link DupeDBClient}. */
    public static Builder client() {
        return new Builder();
    }

    /** Fluent builder for {@link DupeDBClient}. Supports unauthenticated, token (PAT), or OAuth modes. */
    public static class Builder {
        /** Personal access tokens are issued at Settings → Developer on dupedb.net. */
        private static final String PAT_PREFIX = "dupe_pat_";

        private String baseUrl = DEFAULT_BASE_URL;
        private String token;
        private String oauthAppId;
        private String oauthRedirectUri;
        private Path tokenStorePath;

        Builder() {}

        /** Sets the API base URL. */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets a pre-configured Bearer token for headless mode — typically a
         * personal access token ({@code dupe_pat_...}), but any valid Bearer
         * credential works. No browser flow, no refresh, no disk persistence.
         *
         * <p>Prefer {@link #personalAccessToken(String)} when you know the
         * credential is a PAT — it validates the prefix up front.</p>
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Authenticates with a personal access token from
         * <a href="https://dupedb.net/settings/developer">Settings → Developer</a>.
         * The token authenticates as your own account (role {@code user});
         * it never expires and there is no browser flow.
         *
         * <p>Equivalent to {@link #token(String)} plus an eager check that the
         * value looks like a PAT, so a pasted OAuth token or truncated copy
         * fails at build time instead of as a 401 on the first call.</p>
         *
         * @throws IllegalArgumentException if {@code pat} does not start with {@code dupe_pat_}
         */
        public Builder personalAccessToken(String pat) {
            if (pat == null || !pat.startsWith(PAT_PREFIX)) {
                throw new IllegalArgumentException(
                    "Not a personal access token (expected it to start with \"" + PAT_PREFIX
                    + "\"). Create one at https://dupedb.net/settings/developer, or use"
                    + " .token(...) for other Bearer credentials.");
            }
            this.token = pat;
            return this;
        }

        /** Configures OAuth browser flow. Triggered lazily on first authenticated call. */
        public Builder oauth(String appId, String redirectUri) {
            this.oauthAppId = appId;
            this.oauthRedirectUri = redirectUri;
            return this;
        }

        /** Sets a custom token storage path. Default: {@code ~/.dupedb/token.json}. */
        public Builder tokenStore(Path path) {
            this.tokenStorePath = path;
            return this;
        }

        /** Builds the {@link DupeDBClient}. */
        public DupeDBClient build() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalStateException("baseUrl is required");
            }
            if (token != null && oauthAppId != null) {
                throw new IllegalStateException(
                    "Cannot use both .token() and .oauth() -- choose one auth mode");
            }

            AuthManager authManager = null;

            if (token != null) {
                // Direct token mode (D-05)
                authManager = new AuthManager(token);
            } else if (oauthAppId != null) {
                // OAuth browser flow mode (D-04)
                int port = parsePortFromRedirectUri(oauthRedirectUri);
                OAuthFlow flow = new OAuthFlow(baseUrl, oauthAppId, port);
                TokenStore store = tokenStorePath != null
                    ? new TokenStore(tokenStorePath)
                    : new TokenStore();
                authManager = new AuthManager(store, flow);
            }
            // else: unauthenticated mode (public endpoints only)

            return new DupeDBClient(baseUrl, authManager);
        }

        /** Extracts port from redirect URI; defaults to 9876. */
        private static int parsePortFromRedirectUri(String uri) {
            try {
                int port = java.net.URI.create(uri).getPort();
                return port > 0 ? port : 9876;
            } catch (Exception e) {
                return 9876;
            }
        }
    }
}
