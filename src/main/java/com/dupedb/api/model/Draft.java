package com.dupedb.api.model;

import java.util.List;

/** Draft exploit record (not yet submitted). */
public record Draft(
    String id,
    String name,
    String description,
    String type,
    String status,
    String edition,
    String platform,
    String multiplayerType,
    List<String> minecraftVersions,
    List<String> serverIps,
    List<String> pluginServerIps,
    List<String> sources,
    List<String> plugins,
    String pluginName,
    String pluginVersion,
    List<String> serverSoftware,
    List<String> modLinks,
    List<String> embeddedVideos,
    String thumbnail,
    String redirectUrl,
    String accentColor,
    Boolean notifyDiscord,
    List<String> tags,
    boolean isDraft
) {}
