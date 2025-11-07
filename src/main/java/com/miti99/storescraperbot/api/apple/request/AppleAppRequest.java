package com.miti99.storescraperbot.api.apple.request;

public record AppleAppRequest(Long id, String appId, String country, Boolean ratings) {

  public AppleAppRequest(Long id, String country) {
    this(id, null, country, true);
  }

  public AppleAppRequest(String appId, String country) {
    this(null, appId, country, true);
  }
}
