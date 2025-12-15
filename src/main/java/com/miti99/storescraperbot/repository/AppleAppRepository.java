package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.AppleApp;

public class AppleAppRepository extends AbstractCollectionRepository<String, AppleApp> {
  public static final AppleAppRepository INSTANCE = new AppleAppRepository();
}
