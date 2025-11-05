package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.Admin;

/** Đây là repository chỉ chứa 1 key duy nhất, key là "" (rỗng) */
public class AdminRepository extends AbstractRepository<String, Admin> {
  public static final AdminRepository INSTANCE = new AdminRepository();

  protected AdminRepository() {
    super();
  }

  public void init() {
    init("");
  }

  public Admin load() {
    return load("");
  }

  public void save(Admin data) {
    save("", data);
  }
}
