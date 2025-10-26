package com.miti99.storescraperbot.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;

public class JacksonUtil {
  public static ObjectMapper MAPPER = objectMapper();

  private static ObjectMapper objectMapper() {
    var objectMapper = new ObjectMapper();

    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(
        SerializationFeature.FAIL_ON_EMPTY_BEANS,
        SerializationFeature.INDENT_OUTPUT,
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    objectMapper.enable(
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    objectMapper.enable(
        Feature.ALLOW_SINGLE_QUOTES, Feature.ALLOW_UNQUOTED_FIELD_NAMES, Feature.IGNORE_UNDEFINED);
    objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

    objectMapper.setSerializationInclusion(Include.NON_NULL);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

    return objectMapper;
  }

  @SneakyThrows
  public static <T> T readValue(String input, Class<T> valueType) {
    return MAPPER.readValue(input, valueType);
  }

  @SneakyThrows
  public static String writeValueAsString(Object input) {
    return MAPPER.writeValueAsString(input);
  }
}
