package com.miti99.storescraperbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public abstract class AbstractModel<K> {

  @JsonProperty("class")
  protected String clazz = getClass().getSimpleName();
}
