package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.model.Admin;

/**
 * Đây là repository chỉ chứa 1 key duy nhất. Nên lưu trong default collection của Couchbase
 * ("_default")
 *
 * <p>TODO: Refactor các logic của một single key repository sang abstract class để dễ hiểu hơn.
 * Code hiện tại này chỉ là trick tạm thời nên chưa tốt lắm. Cần thiết kế lại tốt hơn
 */
public class AdminRepository extends AbstractRepository<String, Admin> {
  public static final AdminRepository INSTANCE = new AdminRepository();
  public static final String KEY = "admin";

  public AdminRepository() {
    super(Constant.COMMON_COLLECTION_NAME);
  }

  @Override
  public void init(String key) {
    super.init(KEY);
  }

  @Override
  public void save(String key, Admin data) {
    super.save(KEY, data);
  }

  @Override
  public boolean exist(String key) {
    return super.exist(KEY);
  }

  @Override
  public Admin load(String key) {
    return super.load(KEY);
  }

  @Override
  public void delete(String key) {
    super.delete(KEY);
  }

  public void init() {
    init(KEY);
  }

  public Admin load() {
    return load(KEY);
  }

  public void save(Admin data) {
    save(KEY, data);
  }
}
