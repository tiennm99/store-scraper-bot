package com.miti99.storescraperbot.util;

import static com.miti99.storescraperbot.config.Config.REDIS_URL;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisUtil {
  private static final JedisPool REDIS_POOL = new JedisPool(REDIS_URL);

  public static Jedis getJedis() {
    return REDIS_POOL.getResource();
  }
}
