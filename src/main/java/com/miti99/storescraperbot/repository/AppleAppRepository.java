package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.model.AppleApp;
import com.miti99.storescraperbot.util.MongoDBUtil;

public class AppleAppRepository extends AbstractCollectionRepository<String, AppleApp> {
  public static final AppleAppRepository INSTANCE = new AppleAppRepository();

  static {
    // Create TTL index for cached data
    MongoDBUtil.createTTLIndexIfNotExists("appleapp", "clazz", Constant.APP_CACHE_SECONDS);
  }

  @Override
  protected long getExpireSeconds() {
    return Constant.APP_CACHE_SECONDS;
  }
}
