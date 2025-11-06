package com.miti99.storescraperbot.api.google.request;

public record GoogleAppRequest(String appId, String lang, String country) {
  public GoogleAppRequest(String appId) {
    this(appId, "vi", "vn");
  }
}
