package com.miti99.storescraperbot.api.apple.response;

import com.miti99.storescraperbot.api.apple.entity.AppleAppDetail;
import com.miti99.storescraperbot.util.GsonUtil;
import java.util.List;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Value
public class ITunesLookupResponse {
  int resultCount;
  List<AppleAppDetail> results;

  public AppleAppDetail getAppDetail() {
    if (resultCount != 1) {
      log.warn("resultCount('{}') != 1", resultCount);
      log.warn("results: {}", GsonUtil.toJson(results));
    }

    return results.getFirst();
  }
}
