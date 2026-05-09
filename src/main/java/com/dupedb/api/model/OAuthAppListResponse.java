package com.dupedb.api.model;

import java.util.List;

/**
 * Response envelope for {@code GET /api/oauth/my-apps}. {@code quota} is the
 * per-user app limit (default 5; configurable via
 * {@code site_settings.oauth_apps_per_user_quota}).
 */
public record OAuthAppListResponse(List<OAuthApp> apps, int quota) {}
