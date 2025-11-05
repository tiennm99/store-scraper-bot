package com.miti99.storescraperbot.util;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GsonUtil {
  public static final Gson GSON = new Gson();

  public static <T> T fromJson(String input, Class<T> valueType) {
    return GSON.fromJson(input, valueType);
  }

  public static String toJson(Object input) {
    return GSON.toJson(input);
  }
}
