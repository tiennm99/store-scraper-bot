package com.miti99.storescraperbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public abstract class AbstractModel<K> {
  protected K key;

  @JsonProperty("class")
  protected String clazz = getClass().getSimpleName();
}
