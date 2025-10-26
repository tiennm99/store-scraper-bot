package com.miti99.storescraperbot.api.google;

import static org.junit.jupiter.api.Assertions.*;

import com.miti99.storescraperbot.api.google.request.GoogleAppRequest;
import com.miti99.storescraperbot.util.JacksonUtil;
import org.junit.jupiter.api.Test;

class GooglePlayScraperTest {
  @Test
  void testApp() {
    var request = new GoogleAppRequest("vn.kvtm.js");
    var response = GooglePlayScraper.app(request);
    System.out.println(JacksonUtil.writeValueAsString(response));
  }
}
