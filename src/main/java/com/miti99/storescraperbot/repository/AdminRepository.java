package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.Admin;

public class AdminRepository extends AbstractSingletonRepository<String, Admin> {
  public static final AdminRepository INSTANCE = new AdminRepository();
  public static final String KEY = "admin";

  @Override
  protected String getKey() {
    return KEY;
  }
}
