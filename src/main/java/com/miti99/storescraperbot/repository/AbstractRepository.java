package com.miti99.storescraperbot.repository;

import com.couchbase.client.java.Collection;
import com.miti99.storescraperbot.config.Config;
import com.miti99.storescraperbot.model.AbstractModel;
import com.miti99.storescraperbot.util.CouchbaseUtil;
import java.lang.reflect.ParameterizedType;
import lombok.extern.log4j.Log4j2;

/** 1 repository = 1 collection */
@Log4j2
public abstract class AbstractRepository<K, V extends AbstractModel<K>> {
  public static final String SEPARATOR = "_";
  // protected static ObjectMapper objectMapper = new ObjectMapper();
  protected final Class<V> classV = getDataClass();
  // protected final JavaType type = objectMapper.getTypeFactory().constructType(classV);
  protected final String scopeName = Config.ENV.name().toLowerCase();
  protected final String collectionName;

  protected AbstractRepository(String collectionName) {
    this.collectionName = collectionName.toLowerCase();
  }

  public Collection collection() {
    return CouchbaseUtil.BUCKET.scope(scopeName).collection(collectionName);
  }

  /**
   * Lấy ra class của V. Khi tạo 1 abstract class extends AbstractRepository mà không phải final thì
   * sẽ cần override hàm này phù hợp<br>
   * Chi tiết có thể tìm hiểu thêm về getGenericSuperclass và getParameterizedClass<br>
   *
   * @return Class&lt;V&gt;
   */
  protected Class<V> getDataClass() {
    return (Class<V>)
        ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
  }

  public void init(K key) {
    try {
      if (exist(key)) {
        return;
      }
      V data = classV.getDeclaredConstructor().newInstance();
      save(key, data);
    } catch (Exception e) {
      log.error("Error while initializing data", e);
    }
  }

  protected String getDatabaseKey(K key) {
    return String.join(SEPARATOR, classV.getSimpleName(), String.valueOf(key));
  }

  public void save(K key, V data) {
    var databaseKey = getDatabaseKey(key);
    try {
      collection().upsert(databaseKey, data);
    } catch (Exception e) {
      log.error("save error - key {}, databaseKey {}", key, databaseKey, e);
    }
  }

  public boolean exist(K key) {
    var databaseKey = getDatabaseKey(key);
    try {
      return collection().exists(databaseKey).exists();
    } catch (Exception e) {
      log.error("exist error - key {}, databaseKey {}", key, databaseKey, e);
      return false;
    }
  }

  public V load(K key) {
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

  public void delete(K key) {
    var databaseKey = getDatabaseKey(key);
    try {
      collection().remove(databaseKey);
    } catch (Exception e) {
      log.error("delete error", e);
    }
  }
}
