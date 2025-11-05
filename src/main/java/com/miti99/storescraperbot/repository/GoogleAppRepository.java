package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.GoogleApp;

public class GoogleAppRepository extends AbstractRepository<String, GoogleApp> {
  private static final GoogleAppRepository INSTANCE = new GoogleAppRepository();
}
