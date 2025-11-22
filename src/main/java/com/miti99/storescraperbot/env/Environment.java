package com.miti99.storescraperbot.env;

import com.google.common.base.Strings;
import com.miti99.storescraperbot.type.Env;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Environment {
  public static final String MONGODB_CONNECTION_STRING =
      System.getenv("MONGODB_CONNECTION_STRING");
  public static final String MONGODB_USERNAME = System.getenv("MONGODB_USERNAME");
  public static final String MONGODB_PASSWORD = System.getenv("MONGODB_PASSWORD");
  public static final String MONGODB_DATABASE_NAME = System.getenv("MONGODB_DATABASE_NAME");

  public static final String TELEGRAM_BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");
  public static final String TELEGRAM_BOT_USERNAME = System.getenv("TELEGRAM_BOT_USERNAME");

  public static final Env ENV = Env.valueOf(System.getenv("ENV"));
  public static final List<Long> ADMIN_IDS =
      Optional.ofNullable(System.getenv("ADMIN_IDS"))
          .map(
              v ->
                  Arrays.stream(v.split(","))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .map(Long::parseLong)
                      .collect(Collectors.toList()))
          .orElse(Collections.emptyList());
  public static final long CREATOR_ID = ADMIN_IDS.getFirst();

  public static final String SOURCE_COMMIT =
      Strings.isNullOrEmpty(System.getenv("SOURCE_COMMIT"))
          ? "unknown"
          : System.getenv("SOURCE_COMMIT");
}
