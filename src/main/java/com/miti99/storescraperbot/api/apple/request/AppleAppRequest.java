package com.miti99.storescraperbot.api.apple.request;

public record AppleAppRequest(
    Long id,
    String appId,
    String country, // Tạm thời chưa cần phân biệt
    Boolean ratings) {
  public AppleAppRequest(String appId) {
    this(
        null,
        appId,
        "vn",
        true);
  }

  public AppleAppRequest(Long id) {
    this(
        id,
        null,
        "vn",
        true);
  }

  public AppleAppRequest(String appId, String country) {
    this(null, appId, country, true);
  }
}
