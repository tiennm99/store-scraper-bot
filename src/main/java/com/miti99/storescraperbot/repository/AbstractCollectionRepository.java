package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.AbstractModel;
import lombok.extern.log4j.Log4j2;

/**
 * Các repository tương ứng với 1 MongoDB collection, public các method protected ở class
 * AbstractRepository để các nơi khác gọi
 */
@Log4j2
public abstract class AbstractCollectionRepository<K, V extends AbstractModel<K>>
    extends AbstractRepository<K, V> {

  @Override
  public void init(K key) {
    super.init(key);
  }

  @Override
  public void save(K key, V data) {
    super.save(key, data);
  }

  @Override
  public boolean exist(K key) {
    return super.exist(key);
  }

  @Override
  public V load(K key) {
    return super.load(key);
  }

  @Override
  public void delete(K key) {
    super.delete(key);
  }
}
