package com.dupedb.api.model;

/**
 * Plugin reference embedded in {@link ExploitCard#plugins} — name + single
 * version pair, as serialized by the backend metadata.js stripToCardFields
 * path. Distinct from {@link Plugin} (which aggregates multiple versions per
 * name across the whole DB for the {@code /api/plugins} endpoint).
 */
public record PluginRef(String name, String version) {}
