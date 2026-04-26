package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/** Current user's vote state for an exploit. {@code userVote} is {@code "up"}, {@code "down"}, or {@code null}. */
public record Vote(
    @SerializedName("userVote") String userVote
) {}
