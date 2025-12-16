package com.miti99.storescraperbot.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public abstract class AbstractModel {
  @SerializedName("_id")
  protected String id;

  @SerializedName("class")
  protected String clazz = getClass().getSimpleName();

  public void setId(Object id) {
    this.id = String.valueOf(id);
  }
}
