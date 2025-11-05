package com.miti99.storescraperbot.api.google;

import com.miti99.storescraperbot.api.google.request.GoogleAppRequest;
import com.miti99.storescraperbot.util.GsonUtil;
import org.junit.jupiter.api.Test;

class GooglePlayScraperTest {
  @Test
  void testApp() {
    var request = new GoogleAppRequest("vn.kvtm.js");
    var response = GooglePlayScraper.app(request);
    System.out.println(GsonUtil.toJson(response));
  }
}
