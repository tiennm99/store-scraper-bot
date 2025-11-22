package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.model.GoogleApp;
import com.miti99.storescraperbot.util.MongoDBUtil;

public class GoogleAppRepository extends AbstractCollectionRepository<String, GoogleApp> {
  public static final GoogleAppRepository INSTANCE = new GoogleAppRepository();

  static {
    // Create TTL index for cached data
    MongoDBUtil.createTTLIndexIfNotExists("googleapp", "clazz", Constant.APP_CACHE_SECONDS);
  }

  @Override
  protected long getExpireSeconds() {
    return Constant.APP_CACHE_SECONDS;
  }
}
