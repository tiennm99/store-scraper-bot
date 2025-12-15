package com.miti99.storescraperbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Con của AbstractModel phải có getter, setter & field không được final thì mới deserialize được
 */
@Getter
@NoArgsConstructor
@Setter
public abstract class AbstractModel<K> {
  @JsonSetter(nulls = Nulls.SKIP)
  protected K key;

  @JsonProperty("class")
  protected String clazz = getClass().getSimpleName();
}
