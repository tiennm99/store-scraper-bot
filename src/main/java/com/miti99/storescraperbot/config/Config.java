package com.miti99.storescraperbot.config;

import com.miti99.storescraperbot.type.Env;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Config {
  public static final String REDIS_URL = System.getenv("REDIS_URL");

  public static final String TELEGRAM_BOT_TOKEN = System.getenv("TELEGRAM_BOT_TOKEN");
  public static final String TELEGRAM_BOT_USERNAME = System.getenv("TELEGRAM_BOT_USERNAME");

  public static final Env ENV = Env.valueOf(System.getenv("ENV"));
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
