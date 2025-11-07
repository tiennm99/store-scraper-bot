package com.miti99.storescraperbot.api.apple;

import com.miti99.storescraperbot.api.apple.request.AppleAppRequest;
import com.miti99.storescraperbot.util.GsonUtil;
import org.junit.jupiter.api.Test;

class AppStoreScraperTest {
  @Test
  void testApp() {
    var request = new AppleAppRequest("com.mpt.kvtm");
    var response = AppStoreScraper.app(request);
    System.out.println(GsonUtil.toJson(response));
  }

  @Test
  void testComMPTBuraco() {
    var request = new AppleAppRequest("com.mpt.buraco", "mx");
    var response = AppStoreScraper.app(request);
    System.out.println(GsonUtil.toJson(response));
  }
}
