package com.miti99.storescraperbot.api.google;

import com.miti99.storescraperbot.api.google.request.GoogleAppRequest;
import com.miti99.storescraperbot.util.GsonUtil;
import org.junit.jupiter.api.Test;

class GooglePlayScraperTest {
  @Test
  void testApp() {
    var request = new GoogleAppRequest("pool.us");
    var response = GooglePlayScraper.app(request);
    System.out.println(GsonUtil.toJson(response));
  }
}
