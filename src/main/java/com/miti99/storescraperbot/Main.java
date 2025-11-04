package com.miti99.storescraperbot;

import com.miti99.storescraperbot.bot.StoreScrapeBot;
import com.miti99.storescraperbot.config.Config;
import com.miti99.storescraperbot.repository.AdminRepository;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Log4j2
public class Main {

  public static void main(String[] args) {
    AdminRepository.INSTANCE.init();

    try (var botsApplication = new TelegramBotsLongPollingApplication()) {
      botsApplication.registerBot(Config.TELEGRAM_BOT_TOKEN, StoreScrapeBot.INSTANCE);
      log.info("StoreScrapeBot successfully started!");
      Thread.currentThread().join();
    } catch (Exception e) {
      log.error("Error while running bot", e);
    }
  }
}
