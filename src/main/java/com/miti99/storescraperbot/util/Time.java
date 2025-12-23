package com.miti99.storescraperbot.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

public class Time {
  private static final TimeZone TIME_ZONE = TimeZone.getDefault();
  private static final ZoneId ZONE_ID = TIME_ZONE.toZoneId();
  private static long deltaMillis = 0L;

  public static LocalDate currentDate() {
    return LocalDate.ofInstant(currentInstant(), ZONE_ID);
  }

  public static LocalTime currentTime() {
    return LocalTime.ofInstant(currentInstant(), ZONE_ID);
  }

  public static LocalDateTime currentDateTime() {
    return LocalDateTime.ofInstant(currentInstant(), ZONE_ID);
  }

  public static OffsetDateTime currentOffsetDateTime() {
    return OffsetDateTime.ofInstant(currentInstant(), ZONE_ID);
  }

  public static ZonedDateTime currentZonedDateTime() {
    return ZonedDateTime.ofInstant(currentInstant(), ZONE_ID);
  }

  public static Instant currentInstant() {
    return Instant.ofEpochMilli(currentTimeMillis());
  }

  public static long currentTimeMillis() {
    return System.currentTimeMillis() + deltaMillis;
  }

  public static void useMockTime(LocalDateTime dateTime, ZoneId zoneId) {
    deltaMillis = dateTime.atZone(zoneId).toInstant().toEpochMilli() - System.currentTimeMillis();
  }

  public static void useSystemDefaultZoneClock() {
    deltaMillis = 0L;
  }

  private static Clock getClock() {
    return Clock.fixed(currentInstant(), ZONE_ID);
  }
}
