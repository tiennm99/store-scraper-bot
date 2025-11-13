package com.miti99.storescraperbot;

import static com.miti99.storescraperbot.constant.Constant.SCHEDULE_CHECK_APP_TIME;
import static com.miti99.storescraperbot.constant.Constant.SECONDS_PER_DAY;
import static com.miti99.storescraperbot.constant.Constant.VIETNAM_ZONE_ID;
import static com.miti99.storescraperbot.env.Environment.CREATOR_ID;
import static com.miti99.storescraperbot.env.Environment.SOURCE_COMMIT;

import com.miti99.storescraperbot.bot.StoreScrapeBot;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.env.Environment;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.type.Env;
import com.miti99.storescraperbot.util.SchedulerUtil;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Log4j2
public class Main {

  public static void main(String[] args) {
    System.setProperty("log4j2.plugin.scan", "true");

    AdminRepository.INSTANCE.init();

    try (var botsApplication = new TelegramBotsLongPollingApplication()) {
      botsApplication.registerBot(Environment.TELEGRAM_BOT_TOKEN, StoreScrapeBot.INSTANCE);
      log.info("Bot started! Version {}", SOURCE_COMMIT);
      if (Environment.ENV == Env.PRODUCTION) {
        StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
            CREATOR_ID, "Bot started! Version <code>%s</code>".formatted(SOURCE_COMMIT));
      }
      scheduleCheckApp();
      Thread.currentThread().join();
    } catch (Exception e) {
      log.error("Error while running bot", e);
    }
  }

  private static void scheduleCheckApp() {
    var now = LocalDateTime.now();

    var checkTime =
        LocalDateTime.of(LocalDate.now(VIETNAM_ZONE_ID), SCHEDULE_CHECK_APP_TIME)
            .atZone(VIETNAM_ZONE_ID)
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime();
    long initialDelay = Duration.between(now, checkTime).getSeconds();
    if (initialDelay < 0) {
      initialDelay += SECONDS_PER_DAY;
    }
    SchedulerUtil.SCHEDULER.scheduleAtFixedRate(
        StoreScrapeBot.INSTANCE::runCheckApp, initialDelay, SECONDS_PER_DAY, TimeUnit.SECONDS);
  }
}
