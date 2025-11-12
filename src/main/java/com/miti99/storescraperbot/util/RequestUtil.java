package com.miti99.storescraperbot.util;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestUtil {
  private static final Type MAP_STRING_STRING_TYPE =
      new TypeToken<Map<String, String>>() {}.getType();

  private static Map<String, String> objToMapStringString(Object obj) {
    return GsonUtil.GSON.fromJson(GsonUtil.GSON.toJson(obj), MAP_STRING_STRING_TYPE);
  }

  public static String makeGetParams(Map<String, String> params) {
    return params.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("&"));
  }

  public static String makeGetParams(Object obj) {
    return makeGetParams(objToMapStringString(obj));
  }
}
