package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/** A community listing (Discord server, YouTube channel, etc.). */
public record Community(
    int id,
    String platform,
    @SerializedName("platformId") String platformId,
    @SerializedName("platformDisplayName") String platformDisplayName,
    @SerializedName("actionText") String actionText,
    String url,
    String name,
    String description,
    @SerializedName("customDescription") String customDescription,
    @SerializedName("fetchedDescription") String fetchedDescription,
    @SerializedName("displayOrder") int displayOrder,
    @SerializedName("createdAt") String createdAt,
    @SerializedName("updatedAt") String updatedAt,
    @SerializedName("createdByUsername") String createdByUsername,
    @SerializedName("iconUrl") String iconUrl,
    @SerializedName("bannerUrl") String bannerUrl,
    @SerializedName("memberCount") Integer memberCount,
    @SerializedName("extraData") Object extraData,
    @SerializedName("fetchedAt") String fetchedAt,
    @SerializedName("fetchError") String fetchError,
    @SerializedName("isCacheFresh") boolean isCacheFresh,
    @SerializedName("accentColor") String accentColor
) {}
