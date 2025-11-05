package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.model.GoogleApp;

public class GoogleAppRepository extends AbstractRepository<String, GoogleApp> {
  public static final GoogleAppRepository INSTANCE = new GoogleAppRepository();

  @Override
  protected long getExpireSeconds() {
    return Constant.APP_CACHE_SECONDS;
  }
}
