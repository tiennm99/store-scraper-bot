package com.miti99.storescraperbot.model;

import com.miti99.storescraperbot.api.old.google.response.GoogleAppResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleApp extends AbstractModel<String> {
  GoogleAppResponse app;
}
