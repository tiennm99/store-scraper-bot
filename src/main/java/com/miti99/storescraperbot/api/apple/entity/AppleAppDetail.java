package com.miti99.storescraperbot.api.apple.entity;

import lombok.Value;

/**
 * Chỉ define một số field cần thiết nên không đầy đủ. Khi cần thì check raw response để define thêm
 */
@Value
public class AppleAppDetail {
  String bundleId;
  String currentVersionReleaseDate;
  double averageUserRating;
}
