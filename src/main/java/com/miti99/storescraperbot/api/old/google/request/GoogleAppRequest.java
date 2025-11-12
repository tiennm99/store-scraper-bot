package com.miti99.storescraperbot.api.old.google.request;

public record GoogleAppRequest(String appId, String country) {
  public GoogleAppRequest(String appId) {
    this(appId, "vn");
  }
}
