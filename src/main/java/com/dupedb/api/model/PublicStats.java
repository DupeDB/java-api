package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/** Public aggregate statistics (no auth required). */
public record PublicStats(
    @SerializedName("verifiedCount") int verifiedCount,
    @SerializedName("unverifiedCount") int unverifiedCount,
    @SerializedName("userCount") int userCount,
    @SerializedName("sightingsCount") int sightingsCount
) {}
