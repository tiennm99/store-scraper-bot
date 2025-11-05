package com.miti99.storescraperbot.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public abstract class AbstractModel<K> {
  protected K key;

  @SerializedName("class")
  protected String clazz = getClass().getSimpleName();
}
