package com.dupedb.api.model;

/** A point-in-time snapshot of site statistics for sparkline graphs. */
public record StatsSnapshot(String timestamp, int verified, int users, int sightings) {}
