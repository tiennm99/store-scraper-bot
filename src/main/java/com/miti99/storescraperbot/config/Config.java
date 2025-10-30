package com.miti99.storescraperbot.config;

import com.miti99.storescraperbot.type.Env;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Config {
  public static final Env ENV = Env.valueOf(System.getenv("ENV"));

  public static final String COUCHBASE_CONNECTION_STRING =
      System.getenv("COUCHBASE_CONNECTION_STRING");
  public static final String COUCHBASE_USERNAME = System.getenv("COUCHBASE_USERNAME");
  public static final String COUCHBASE_PASSWORD = System.getenv("COUCHBASE_PASSWORD");
  public static final String COUCHBASE_BUCKET_NAME = System.getenv("COUCHBASE_BUCKET_NAME");

  public static final String TELEGRAM_BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");
  public static final String TELEGRAM_BOT_USERNAME = System.getenv("TELEGRAM_BOT_USERNAME");

  public static final Long CREATOR_ID = Long.parseLong(System.getenv("CREATOR_ID"));
  public static final Set<Long> ADMIN_IDS =
      Optional.ofNullable(System.getenv("ADMIN_IDS"))
          .map(
              v ->
                  Arrays.stream(v.split(","))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .map(Long::parseLong)
                      .collect(Collectors.toSet()))
          .orElse(Collections.emptySet());
}
