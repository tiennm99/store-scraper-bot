package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.model.AppleApp;

public class AppleAppRepository extends AbstractCollectionRepository<String, AppleApp> {
  public static final AppleAppRepository INSTANCE = new AppleAppRepository();

  @Override
  protected long getExpireSeconds() {
    return Constant.APP_CACHE_SECONDS;
  }
}
