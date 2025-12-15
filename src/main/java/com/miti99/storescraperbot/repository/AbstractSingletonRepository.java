package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.AbstractModel;
import lombok.extern.log4j.Log4j2;

/**
 * Repository chỉ chứa 1 key duy nhất, public các method liên quan nhưng không cho truyền params
 * vào. Các repository loại này được lưu trong 1 collection duy nhất là "common"
 */
@Log4j2
public abstract class AbstractSingletonRepository<K, V extends AbstractModel<K>>
    extends AbstractRepository<K, V> {

  public static final String COMMON_COLLECTION_NAME = "common";
  protected final K key = getKey();

  protected AbstractSingletonRepository() {
    super(COMMON_COLLECTION_NAME);
  }

  protected abstract K getKey();

  public void init() {
    init(key);
  }

  public void save(V data) {
    save(key, data);
  }

  public boolean exist() {
    return exist(key);
  }

  public V load() {
    return load(key);
  }

  public void delete() {
    delete(key);
  }
}
