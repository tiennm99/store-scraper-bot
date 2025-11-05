package com.miti99.storescraperbot.model;

import com.miti99.storescraperbot.api.google.response.GoogleAppResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleApp extends AbstractModel<String> {
  long cacheTime;
  GoogleAppResponse rawResponse;

  public GoogleApp(String key) {
    super(key);
  }
}
