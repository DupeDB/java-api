package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/** Response after voting on an exploit. {@code userVote} is {@code "up"}, {@code "down"}, or {@code null}. */
public record VoteResult(
    int upvotes,
    int downvotes,
    @SerializedName("userVote") String userVote
) {}
