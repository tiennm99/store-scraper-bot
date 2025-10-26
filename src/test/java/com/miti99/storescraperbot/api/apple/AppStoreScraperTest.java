package com.miti99.storescraperbot.api.apple;

import static org.junit.jupiter.api.Assertions.*;

import com.miti99.storescraperbot.api.apple.request.AppleAppRequest;
import com.miti99.storescraperbot.util.JacksonUtil;
import org.junit.jupiter.api.Test;

class AppStoreScraperTest {
  @Test
  void testApp() {
    var request = new AppleAppRequest("com.mpt.kvtm");
    var response = AppStoreScraper.app(request);
    System.out.println(JacksonUtil.writeValueAsString(response));
  }
}
