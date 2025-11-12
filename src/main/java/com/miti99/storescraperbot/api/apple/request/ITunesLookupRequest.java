package com.miti99.storescraperbot.api.apple.request;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
@Value
public class ITunesLookupRequest {
  Long id;
  String bundleId;
  String entity = "software";

  public ITunesLookupRequest(Long id) {
    this(id, null);
  }

  public ITunesLookupRequest(String bundleId) {
    this(null, bundleId);
  }
}
