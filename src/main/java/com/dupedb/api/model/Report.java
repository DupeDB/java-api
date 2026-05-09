package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * An exploit report row from {@code GET /api/new-reports?since=<ISO>}.
 *
 * <p>{@code reportType} is one of {@code patched}, {@code custom},
 * {@code disclosure}, {@code inaccurate}, {@code miscredit}.
 * {@code reporterUsername} is {@code null} for reports submitted before
 * the reporter relationship was added (LEFT JOIN nullable).
 */
public record Report(
    int id,
    @SerializedName("exploit_id") String exploitId,
    @SerializedName("report_type") String reportType,
    String message,
    @SerializedName("created_at") String createdAt,
    @SerializedName("exploit_name") String exploitName,
    @SerializedName("reporter_username") String reporterUsername
) {}
