# DupeDB-API

[![](https://jitpack.io/v/DupeDB/api.svg)](https://jitpack.io/#DupeDB/api)

Java library for the DupeDB Minecraft exploit database API. 

## Requirements

- Java 25+

## Installation

Add the JitPack repository and dependency:

**Gradle (Kotlin DSL):**
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.DupeDB:api:1.0.2")
}
```

**Gradle (Groovy DSL):**
```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle
dependencies {
    implementation 'com.github.DupeDB:api:1.0.2'
}
```

## OAuth App Registration

Authenticated endpoints require an OAuth app. Apps are free and self-service — there's no approval step.

1. **Sign in** at [dupedb.net](https://dupedb.net) with Discord.
2. Open **[Settings → App settings](https://dupedb.net/settings/oauth-apps)** and create a new app.
3. Fill in the form:
   - **App ID (slug)** — your app's permanent identifier. 3–32 characters, lowercase letters, numbers, and dashes (e.g. `my-java-app`). It **cannot be changed later** and cannot start with a reserved word (`dupedb`, `admin`, `staff`, `mod`, `official`, `system`, `bot`).
   - **Display Name** — the name shown to users on the consent screen (e.g. `My Java App`).
   - **Permission Level** — *Full Access*, or *Read Only* (browse only — a read-only app cannot vote, comment, submit drafts, or upload).
   - **App Type** — choose **Desktop / CLI** for a mod, plugin, bot, or desktop tool that uses this library. The loopback redirect `http://127.0.0.1/callback` is registered for you, and its port is wildcarded at runtime so any free local port works. Choose **Web App** only if you handle the OAuth callback on your own HTTPS server.
4. Copy the **App ID** — that's the only value the library needs. This is a public PKCE client, so there is **no client secret**.

You can register up to 5 apps per account and manage them anytime from the same page (or programmatically via `client.myApps()`). See the full [Developer Documentation](https://dupedb.net/resource/developer-documentation) for the underlying OAuth flow and token handling.

## Usage

### Unauthenticated (public endpoints only)
```java
DupeDBClient client = DupeDB.client().build();

PublicStats stats = client.metadata().publicStats();
ExploitMeta meta = client.exploits().getMeta("abc12345678");
```

### Authenticated with token (headless servers)
```java
DupeDBClient client = DupeDB.client()
    .token("dupe_your_token_here")
    .build();

SearchResult<ExploitCard> results = client.exploits().search("elytra", 1);
```

### Authenticated with OAuth (desktop/mod usage)

Pass the **App ID** from registration. The redirect URI must be a `127.0.0.1` loopback URL: the library starts a local listener on its port, opens the user's browser to approve access, then captures the callback and exchanges it for a token. The token is written to `tokenStore` and refreshed automatically, so the user only approves once.

```java
DupeDBClient client = DupeDB.client()
    .oauth("my-java-app", "http://127.0.0.1:9876/callback")
    .tokenStore(Path.of("config/dupedb-token.json"))
    .build();

// First authenticated call opens the browser; later runs reuse the saved token.
User me = client.user().me();
```

> Use `127.0.0.1`, not `localhost` — the server matches the loopback host exactly (only the port is wildcarded). Any free port works; `9876` is just an example. Omit `.tokenStore(...)` to fall back to the default `~/.dupedb/token.json`.

### Which auth mode to use

| Context | Auth Mode | Why |
|---------|-----------|-----|
| Fabric/Forge client mod | OAuth | Player is at their desktop so browser flow works naturally. Token is saved so the player only authenticates once. |
| Read-only / public data | None | A handful of endpoints (health, version, public stats, exploit meta, site visibility) need no auth. Most metadata and detail lookups now require a token. |

### Threading

All API calls are **blocking HTTP requests**. Never call them on Minecraft's main thread as it might freeze the game or trigger the server anti-cheat.

```java
// Fabric example -- run off the main thread
CompletableFuture.supplyAsync(() -> {
    try {
        return client.exploits().getById("abc12345678");
    } catch (DupeDBException e) {
        throw new RuntimeException(e);
    }
}).thenAccept(exploit -> {
    // Back on any thread -- use MinecraftClient.execute() if you need the render thread
    System.out.println(exploit.name());
});
```

### Error handling
```java
try {
    Exploit exploit = client.exploits().getById("abc12345678");
} catch (AuthException e) {
    // Token expired or invalid
} catch (RateLimitException e) {
    // Too many requests -- retry after e.getRetryAfterSeconds()
} catch (ApiException e) {
    // HTTP error: e.getStatusCode(), e.getMessage()
} catch (NetworkException e) {
    // Connection failed
}
```

## API Reference

### Exploits

| Method | Endpoint | Auth |
|--------|----------|------|
| `exploits().search(query, page)` | GET /api/exploits/search | Yes |
| `exploits().search(query, page, filters)` | GET /api/exploits/search | Yes |
| `exploits().search(query, page, SearchFilters)` | GET /api/exploits/search | Yes |
| `exploits().getById(id)` | GET /api/exploits/:id | Yes |
| `exploits().getMeta(id)` | GET /api/exploits/:id/meta | No |
| `exploits().update(id, data)` | PUT /api/exploits/:id | Yes |
| `exploits().report(id, type, message)` | POST /api/exploits/:id/report | Yes |

### Votes

| Method | Endpoint | Auth |
|--------|----------|------|
| `votes().get(exploitId)` | GET /api/exploits/:id/vote | Yes |
| `votes().vote(exploitId, type)` | POST /api/exploits/:id/vote | Yes |
| `votes().clear(exploitId)` | POST /api/exploits/:id/vote | Yes |

### Comments

| Method | Endpoint | Auth |
|--------|----------|------|
| `comments().list(exploitId)` | GET /api/exploits/:id/comments | Yes |
| `comments().add(exploitId, content)` | POST /api/exploits/:id/comments | Yes |
| `comments().add(exploitId, content, parentId, isSighting, serverIp)` | POST /api/exploits/:id/comments | Yes |
| `comments().edit(commentId, content)` | PUT /api/auth/my-comments/:id | Yes |
| `comments().deleteOwn(commentId)` | DELETE /api/auth/my-comments/:id | Yes |

### Sightings

Cross-exploit community sighting browse. For your own sightings see `user().mySightings()`.

| Method | Endpoint | Auth |
|--------|----------|------|
| `sightings().search(query, page)` | GET /api/sightings/search | Yes |
| `sightings().search(query, page, filters)` | GET /api/sightings/search | Yes |
| `sightings().search(query, page, SightingFilters)` | GET /api/sightings/search | Yes |
| `sightings().autocomplete(prefix)` | GET /api/sightings/search?autocomplete=1 | Yes |

Exploit and sighting search expose the full set of filters and sort options from the
site's filter sidebar via the type-safe `SearchFilters` / `SightingFilters` builders:

```java
// Exploits -- same knobs as the browse page filter sidebar
client.exploits().search("", 1, new SearchFilters()
    .status("verified,working").edition("java").version("1.21.4")
    .sort("upvotes").order("desc"));

// Sightings -- status, server IP, player-count range, sort
client.sightings().search("", 1, new SightingFilters()
    .status("working").playerMin(20)
    .sort("verified_player_count").order("desc"));
```

### Drafts

| Method | Endpoint | Auth |
|--------|----------|------|
| `drafts().getCurrent()` | GET /api/exploits/draft | Yes |
| `drafts().create(data)` | POST /api/exploits/draft | Yes |
| `drafts().update(id, data)` | PUT /api/exploits/draft/:id | Yes |
| `drafts().delete(id)` | DELETE /api/exploits/draft/:id | Yes |
| `drafts().submit(id)` | POST /api/exploits/draft/:id/submit | Yes |

`create(data)` and `update(id, data)` return the saved `Draft` (each re-fetches the
draft after writing, since the create/update endpoints themselves return only an id
or a status message). Only one draft exists per account. A full lifecycle looks like:

```java
Draft draft = client.drafts().create(Map.of("name", "My dupe", "edition", "java"));
draft = client.drafts().update(draft.id(), Map.of("description", "Steps..."));
client.drafts().submit(draft.id());
```

### Current User

| Method | Endpoint | Auth |
|--------|----------|------|
| `user().me()` | GET /api/auth/me | Yes |
| `user().updateDisplayName(name)` | PUT /api/auth/display-name | Yes |
| `user().updatePrivacy(hide)` | PUT /api/auth/privacy-settings | Yes |
| `user().myExploits()` | GET /api/auth/my-exploits | Yes |
| `user().myComments()` | GET /api/auth/my-comments | Yes |
| `user().mySightings()` | GET /api/auth/my-sightings | Yes |
| `user().deleteExploit(id)` | DELETE /api/auth/my-exploits/:id | Yes |
| `user().connectedApps()` | GET /api/oauth/connected | Yes |
| `user().revokeApp(appId)` | DELETE /api/oauth/connected/:appId | Yes |
| `user().deleteAccount()` | DELETE /api/auth/account | Yes |

### User Profiles

| Method | Endpoint | Auth |
|--------|----------|------|
| `users().getProfile(userId)` | GET /api/users/:id/profile | Yes |
| `users().lookup(name)` | GET /api/users/lookup/:name | Yes |
| `users().getProfileByDiscordId(discordId)` | GET /api/users/discord/:discordId/profile | Yes |
| `users().getContributionsByDiscordId(discordId, limit, offset)` | GET /api/users/discord/:discordId/contributions | Yes |
| `users().discordPoints()` | GET /api/users/discord-points | Yes |
| `users().search(query)` | GET /api/users/search | Yes |

### My Apps (OAuth Self-Service)

| Method | Endpoint | Auth |
|--------|----------|------|
| `myApps().list()` | GET /api/oauth/my-apps | Yes |
| `myApps().create(id, name, redirectUris, readOnly)` | POST /api/oauth/my-apps | Yes |
| `myApps().update(id, name, redirectUris, readOnly)` | PATCH /api/oauth/my-apps/:id | Yes |
| `myApps().delete(id)` | DELETE /api/oauth/my-apps/:id | Yes |

### OAuth

| Method | Endpoint | Auth |
|--------|----------|------|
| `oauth().revoke(token, tokenTypeHint)` | POST /api/oauth/revoke | No |

### Communities

| Method | Endpoint | Auth |
|--------|----------|------|
| `communities().list()` | GET /api/communities/ | No |
| `communities().detect(url)` | GET /api/communities/detect | No |
| `communities().getById(id)` | GET /api/communities/:id | No |

### Media

| Method | Endpoint | Auth |
|--------|----------|------|
| `media().uploadProfilePicture(file)` | POST /api/auth/profile-picture | Yes |
| `media().deleteProfilePicture()` | DELETE /api/auth/profile-picture | Yes |
| `media().uploadMedia(file)` | POST /api/upload/media | Yes |
| `media().uploadMedia(file, draftId, exploitId, uploadType, resourceId)` | POST /api/upload/media | Yes |
| `media().deleteMedia(filename)` | DELETE /api/upload/media/:filename | Yes |

### Resources

| Method | Endpoint | Auth |
|--------|----------|------|
| `resources().categories()` | GET /api/resources/categories | No |
| `resources().list()` | GET /api/resources/ | No |
| `resources().list(categoryId)` | GET /api/resources/?category=:id | No |
| `resources().getById(id)` | GET /api/resources/id/:id | No |
| `resources().getBySlug(slug)` | GET /api/resources/:slug | No |

### Metadata

| Method | Endpoint | Auth |
|--------|----------|------|
| `metadata().health()` | GET /api/health | No |
| `metadata().version()` | GET /api/version | No |
| `metadata().tags()` | GET /api/tags | Yes |
| `metadata().versions()` | GET /api/versions | Yes |
| `metadata().types()` | GET /api/types | Yes |
| `metadata().stats()` | GET /api/stats | Yes |
| `metadata().latestActivity()` | GET /api/latest-activity | Yes |
| `metadata().serverIps(page, limit)` | GET /api/server-ips | Yes |
| `metadata().plugins()` | GET /api/plugins | Yes |
| `metadata().newPublished(since)` | GET /api/new-published | Yes |
| `metadata().newUnverified(since)` | GET /api/new-unverified | Yes |
| `metadata().exploitCardBatch(ids)` | GET /api/exploits-card-batch | Yes |
| `metadata().newUnverifiedSightings(since)` | GET /api/new-unverified-sightings | Yes |
| `metadata().newVerifiedSightings(since)` | GET /api/new-verified-sightings | Yes |
| `metadata().newReports(since)` | GET /api/new-reports | Yes |
| `metadata().newAuditLogs(since)` | GET /api/new-audit-logs | Yes |
| `metadata().publicStats()` | GET /api/public/stats | No |
| `metadata().publicStatsHistory(days)` | GET /api/public/stats/history | No |
| `metadata().siteVisibility()` | GET /api/site-visibility | No |
| `metadata().publicExploits()` | GET /api/public/exploits | No |

> `metadata().serverTypes()` is `@Deprecated` — the server returns an empty array. Read `Exploit.serverSoftware()` (per-exploit) instead.

## Rate Limits

| Endpoint Group | Limit |
|----------------|-------|
| General | 200 / 15 min |
| Search (browse) | 30 / min |
| Detail | 15 / min |
| Voting | 60 / min |
| Comments | 10 / min |
| Reports | 5 / min |
| Draft submit | 20 / hour |
| Draft auto-save | 30 / min |
