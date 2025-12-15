package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.GoogleApp;

public class GoogleAppRepository extends AbstractCollectionRepository<String, GoogleApp> {
  public static final GoogleAppRepository INSTANCE = new GoogleAppRepository();
}
