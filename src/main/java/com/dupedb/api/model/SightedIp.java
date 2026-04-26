package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/** A community-verified server IP for an exploit, paired with the comment that introduced it. */
public record SightedIp(
    String ip,
    @SerializedName("commentId") int commentId
) {}
