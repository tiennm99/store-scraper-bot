package com.miti99.storescraperbot;

import static com.miti99.storescraperbot.env.Environment.CREATOR_ID;
import static com.miti99.storescraperbot.env.Environment.SOURCE_COMMIT;

import com.miti99.storescraperbot.bot.StoreScrapeBot;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.env.Environment;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.type.Env;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Log4j2
public class Main {

  public static void main(String[] args) {
    AdminRepository.INSTANCE.init();

    try (var botsApplication = new TelegramBotsLongPollingApplication()) {
      botsApplication.registerBot(Environment.TELEGRAM_BOT_TOKEN, StoreScrapeBot.INSTANCE);
      log.info("StoreScrapeBot successfully started!");
      if (Environment.ENV == Env.PRODUCTION) {
        StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
            CREATOR_ID, "Bot started! Version <code>%s</code>".formatted(SOURCE_COMMIT));
      }
      Thread.currentThread().join();
    } catch (Exception e) {
      log.error("Error while running bot", e);
    }
  }
}
