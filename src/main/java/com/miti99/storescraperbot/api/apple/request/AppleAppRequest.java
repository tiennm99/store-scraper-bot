package com.miti99.storescraperbot.api.apple.request;

public record AppleAppRequest(String appId, Long id) {
  public AppleAppRequest(String appId) {
    this(appId, null);
  }

  public AppleAppRequest(Long id) {
    this(null, id);
  }
}
