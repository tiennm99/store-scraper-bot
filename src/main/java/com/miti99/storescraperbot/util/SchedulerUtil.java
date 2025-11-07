package com.miti99.storescraperbot.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SchedulerUtil {
  public static final ScheduledExecutorService SCHEDULER =
      Executors.newSingleThreadScheduledExecutor();
}
