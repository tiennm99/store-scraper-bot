package com.miti99.storescraperbot.constant;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class Constant {
  public static final long APP_CACHE_SECONDS = 600;
  public static final long APP_CACHE_MILLIS = APP_CACHE_SECONDS * 1000;
  public static final long NUM_DAYS_WARNING_NOT_UPDATED = 30;
  public static final LocalTime SCHEDULE_CHECK_APP_TIME = LocalTime.of(7, 0);

  public static final String VIETNAM_ZONE_ID_STRING = "Asia/Ho_Chi_Minh";
  public static final ZoneId VIETNAM_ZONE_ID = ZoneId.of(VIETNAM_ZONE_ID_STRING);
  public static final long SECONDS_PER_DAY = ChronoUnit.DAYS.getDuration().getSeconds();
  public static final Set<DayOfWeek> WEEKENDS = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

  public static final String DEFAULT_DATABASE_NAME = "store-scraper-bot";
}
