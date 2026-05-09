package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/** Site version information from {@code /api/version}. */
public record VersionInfo(
    @SerializedName("commitCount") int commitCount,
    @SerializedName("lastCommitMessage") String lastCommitMessage,
    @SerializedName("lastCommitDate") String lastCommitDate,
    @SerializedName("authorName") String authorName,
    /** Derived server-side from the commit author's {@code users.noreply.github.com} email; empty string when not derivable. */
    @SerializedName("githubUsername") String githubUsername
) {}
