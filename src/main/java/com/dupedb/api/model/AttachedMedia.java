package com.dupedb.api.model;

import com.google.gson.annotations.SerializedName;

/** A media file currently attached to an exploit. */
public record AttachedMedia(
    String filename,
    @SerializedName("original_name") String originalName,
    @SerializedName("file_path") String filePath,
    @SerializedName("file_size") long fileSize,
    String mimetype
) {}
