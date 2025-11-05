package com.miti99.storescraperbot.repository;

import com.google.common.base.CaseFormat;
import com.miti99.storescraperbot.config.Config;
import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.model.AbstractModel;
import com.miti99.storescraperbot.util.GsonUtil;
import com.miti99.storescraperbot.util.RedisUtil;
import java.lang.reflect.ParameterizedType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import redis.clients.jedis.params.SetParams;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractRepository<K, V extends AbstractModel<K>> {
  public static final String SEPARATOR = ":";
  // protected final Class<K> classK = getKeyClass();
  protected final Class<V> classV = getDataClass();
  protected final String prefix =
      String.join(
          SEPARATOR,
          Constant.APP_NAME,
          Config.ENV.name().toLowerCase(),
          CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, classV.getSimpleName()));

  // protected Class<K> getKeyClass() {
  //   return (Class<K>)
  //       ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  // }

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

  /**
   * @return expire seconds. <= 0 mean never expire.
   */
  protected long getExpireSeconds() {
    return 0;
  }

  public void init(K key) {
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
    return String.join(SEPARATOR, prefix, String.valueOf(key));
  }

  public void save(K key, V data) {
    var databaseKey = getDatabaseKey(key);
    try (var jedis = RedisUtil.getJedis()) {
      var json = GsonUtil.toJson(data);
      if (getExpireSeconds() <= 0) {
        jedis.set(databaseKey, json);
      } else {
        jedis.set(databaseKey, json, SetParams.setParams().ex(getExpireSeconds()));
      }
    } catch (Exception e) {
      log.error("save error - key {}, databaseKey {}", key, databaseKey, e);
    }
  }

  public boolean exist(K key) {
    var databaseKey = getDatabaseKey(key);
    try (var jedis = RedisUtil.getJedis()) {
      return jedis.exists(databaseKey);
    } catch (Exception e) {
      log.error("exist error - key {}, databaseKey {}", key, databaseKey, e);
      return false;
    }
  }

  public V load(K key) {
    var databaseKey = getDatabaseKey(key);
    try (var jedis = RedisUtil.getJedis()) {
      var json = jedis.get(databaseKey);
      return GsonUtil.fromJson(json, classV);
    } catch (Exception e) {
      log.error("load error - key {}, databaseKey {}", key, databaseKey, e);
      return null;
    }
  }

  public void delete(K key) {
    var databaseKey = getDatabaseKey(key);
    try (var jedis = RedisUtil.getJedis()) {
      jedis.del(databaseKey);
    } catch (Exception e) {
      log.error("delete error", e);
    }
  }
}
