package com.miti99.storescraperbot.api.old.apple;

import com.miti99.storescraperbot.api.old.apple.AppStoreScraper;
import com.miti99.storescraperbot.api.old.apple.request.AppleAppRequest;
import com.miti99.storescraperbot.util.GsonUtil;
import org.junit.jupiter.api.Test;

class AppStoreScraperTest {
  @Test
  void testComMptKvtm() {
    var request = new AppleAppRequest("com.mpt.kvtm", "vn");
    var response = AppStoreScraper.app(request);
    System.out.println(GsonUtil.toJson(response));
  }

  @Test
  void testComMptBuraco() {
    var request = new AppleAppRequest("com.mpt.buraco", "mx");
    var response = AppStoreScraper.app(request);
    System.out.println(GsonUtil.toJson(response));
  }

  @Test
  void testComMptDoudizhu() {
    var request = new AppleAppRequest("com.mpt.doudizhu", "hk");
    var response = AppStoreScraper.app(request);
    System.out.println(GsonUtil.toJson(response));
  }
}
