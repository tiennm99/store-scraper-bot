package com.miti99.storescraperbot.repository;

import static com.miti99.storescraperbot.repository.AbstractSingletonRepository.COMMON_COLLECTION_NAME;

import com.google.common.base.CaseFormat;
import com.miti99.storescraperbot.model.AbstractModel;
import com.miti99.storescraperbot.util.MongoDBUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import java.lang.reflect.ParameterizedType;
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
  protected final String collectionName;

  protected AbstractRepository(String collectionName) {
    this.collectionName = collectionName;
    MongoDBUtil.createCollectionIfNotExists(collectionName);
  }

  protected AbstractRepository() {
    collectionName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, classV.getSimpleName());
    if (COMMON_COLLECTION_NAME.equals(collectionName)) {
      throw new RuntimeException(
          "Collection named '%s' is reserved".formatted(COMMON_COLLECTION_NAME));
    }
    MongoDBUtil.createCollectionIfNotExists(collectionName);
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

  protected MongoCollection<V> collection() {
    return MongoDBUtil.DATABASE.getCollection(collectionName, classV);
  }

  protected void init(K key) {
    try {
      if (exist(key)) {
        return;
      }
      V data = classV.getDeclaredConstructor().newInstance();
      data.setKey(key);
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
      collection()
          .replaceOne(Filters.eq("_id", databaseKey), data, new ReplaceOptions().upsert(true));
    } catch (Exception e) {
      log.error("save error - key {}, databaseKey {}", key, databaseKey, e);
    }
  }

  protected boolean exist(K key) {
    var databaseKey = getDatabaseKey(key);
    try {
      return collection().countDocuments(Filters.eq("_id", databaseKey)) > 0;
    } catch (Exception e) {
      log.error("exist error - key {}, databaseKey {}", key, databaseKey, e);
      return false;
    }
  }

  protected V load(K key) {
    var databaseKey = getDatabaseKey(key);
    try {
      return collection().find(Filters.eq("_id", databaseKey)).first();
    } catch (Exception e) {
      log.error("load error - key {}, databaseKey {}", key, databaseKey, e);
      return null;
    }
  }

  protected void delete(K key) {
    var databaseKey = getDatabaseKey(key);
    try {
      collection().deleteOne(Filters.eq("_id", databaseKey));
    } catch (Exception e) {
      log.error("delete error", e);
    }
  }
}
