package com.miti99.storescraperbot.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class AbstractModel<K> {
  final K key;

  @SerializedName("class")
  protected String clazz = getClass().getSimpleName();
}
