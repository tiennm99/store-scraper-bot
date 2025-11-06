package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.Admin;

/** Đây là repository chỉ chứa 1 key duy nhất, key là "admin" */
public class AdminRepository extends AbstractRepository<String, Admin> {
  public static final AdminRepository INSTANCE = new AdminRepository();

  public void init() {
    init("admin");
  }

  public Admin load() {
    return load("admin");
  }

  public void save(Admin data) {
    save("admin", data);
  }
}
