package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Display metadata (icon + total download count) for one required plugin of a
 * plugin-type exploit. Returned by {@code GET /api/exploits/:id/plugins-meta}.
 *
 * <p>The response array is <b>positional</b> — entry {@code i} describes entry
 * {@code i} of the exploit's plugin list; the endpoint returns no names itself.
 *
 * <p>Both fields are {@code null} when the plugin has no recognized provider
 * URL (Modrinth/SpigotMC) or the provider lookup missed. Values are served from
 * a short-lived server cache, so download counts may lag live numbers.
 */
public record PluginMeta(
    @SerializedName("iconUrl") String iconUrl,
    Long downloads
) {}
