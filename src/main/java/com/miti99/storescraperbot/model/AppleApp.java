package com.miti99.storescraperbot.model;

import com.miti99.storescraperbot.api.apple.response.AppleAppResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppleApp extends AbstractModel<String> {
  AppleAppResponse app;
}
