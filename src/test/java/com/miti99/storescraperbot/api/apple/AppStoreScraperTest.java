package com.miti99.storescraperbot.api.apple;

import static org.junit.jupiter.api.Assertions.*;

import com.miti99.storescraperbot.util.GsonUtil;
import org.junit.jupiter.api.Test;

class AppStoreScraperTest {
  @Test
  void testComMptKvtm() {
    var response = AppStoreScraper.app("com.mpt.kvtm");
    System.out.println(GsonUtil.toJson(response));
  }

  @Test
  void testComMptBuraco() {
    // var response = AppStoreScraper.app("com.mpt.buraco");
    var response = AppStoreScraper.app(1638264682);
    System.out.println(GsonUtil.toJson(response));
  }

  @Test
  void testComMptDoudizhu() {
    var response = AppStoreScraper.app("com.mpt.doudizhu");
    System.out.println(GsonUtil.toJson(response));
  }
}
