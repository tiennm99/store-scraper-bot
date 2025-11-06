package com.miti99.storescraperbot.api.apple.request;

public record AppleAppRequest(String appId, Long id, Boolean ratings) {
  public AppleAppRequest(String appId) {
    this(appId, null, true);
  }

  public AppleAppRequest(Long id) {
    this(null, id, true);
  }
}
