package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.AppleApp;
import com.miti99.storescraperbot.model.Group;

public class AppleAppRepository extends AbstractRepository<String, AppleApp> {
  public static final AppleAppRepository INSTANCE = new AppleAppRepository();

  protected AppleAppRepository() {
    super("apple");
  }
}
