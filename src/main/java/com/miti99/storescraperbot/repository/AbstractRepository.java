package com.miti99.storescraperbot.repository;

import static com.miti99.storescraperbot.repository.AbstractSingletonRepository.COMMON_COLLECTION_NAME;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.UpsertOptions;
import com.google.common.base.CaseFormat;
import com.miti99.storescraperbot.env.Environment;
import com.miti99.storescraperbot.model.AbstractModel;
import com.miti99.storescraperbot.util.CouchbaseUtil;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import lombok.extern.log4j.Log4j2;

/**
 * @param <K> class Key
 * @param <V> class Value
 */
@Log4j2
public abstract class AbstractRepository<K, V extends AbstractModel<K>> {
  protected static final String SEPARATOR = "_";
  // protected final Class<K> classK = getKeyClass();
  protected final Class<V> classV = getDataClass();
  protected final String scopeName = Environment.ENV.name().toLowerCase();
  protected final String collectionName;

  protected AbstractRepository(String collectionName) {
    this.collectionName = collectionName;
    CouchbaseUtil.createCollection(scopeName, collectionName);
  }

  protected AbstractRepository() {
    collectionName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, classV.getSimpleName());
    if (COMMON_COLLECTION_NAME.equals(collectionName)) {
      throw new RuntimeException(
          "Collection named '%s' is reserved".formatted(COMMON_COLLECTION_NAME));
    }
    CouchbaseUtil.createCollection(scopeName, collectionName);
  }

  // protected Class<K> getKeyClass() {
  //   return (Class<K>)
  //       ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  // }

  /**
   * Lấy ra class của V. Khi tạo 1 abstract class extends AbstractRepository mà không phải final thì
   * sẽ cần override hàm này phù hợp
   *
   * @return Class&lt;V&gt;
   */
  protected Class<V> getDataClass() {
    return (Class<V>)
        ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
  }

  protected Collection collection() {
    return CouchbaseUtil.BUCKET.scope(scopeName).collection(collectionName);
  }

  /**
   * @return expire seconds. 0 mean never expire.
   */
  protected long getExpireSeconds() {
    return 0;
  }

  protected void init(K key) {
    try {
      if (exist(key)) {
        return;
      }
      V data = classV.getDeclaredConstructor().newInstance();
      data.key(key);
      save(key, data);
    } catch (Exception e) {
      log.error("Error while initializing data", e);
    }
  }

  protected String getDatabaseKey(K key) {
    return String.valueOf(key);
  }

  protected void save(K key, V data) {
    var databaseKey = getDatabaseKey(key);
    try {
      if (getExpireSeconds() == 0) {
        collection().upsert(databaseKey, data);
      } else {
        var upsertOptions =
            UpsertOptions.upsertOptions().expiry(Duration.ofSeconds(getExpireSeconds()));
        collection().upsert(databaseKey, data, upsertOptions);
      }
    } catch (Exception e) {
      log.error("save error - key {}, databaseKey {}", key, databaseKey, e);
    }
  }

  protected boolean exist(K key) {
    var databaseKey = getDatabaseKey(key);
    try {
      return collection().exists(databaseKey).exists();
    } catch (Exception e) {
      log.error("exist error - key {}, databaseKey {}", key, databaseKey, e);
      return false;
    }
  }

  protected V load(K key) {
    var databaseKey = getDatabaseKey(key);
    try {
      var getResult = collection().get(databaseKey);
      if (getResult == null) {
        return null;
      }
      return getResult.contentAs(classV);
    } catch (Exception e) {
      log.error("load error - key {}, databaseKey {}", key, databaseKey, e);
      return null;
    }
  }

  protected void delete(K key) {
    var databaseKey = getDatabaseKey(key);
    try {
      collection().remove(databaseKey);
    } catch (Exception e) {
      log.error("delete error", e);
    }
  }
}
