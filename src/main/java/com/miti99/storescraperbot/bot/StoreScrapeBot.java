package com.miti99.storescraperbot.bot;

import com.miti99.storescraperbot.bot.command.AddGroupCommand;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.extensions.bots.commandbot.CommandLongPollingTelegramBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Log4j2
public class StoreScrapeBot extends CommandLongPollingTelegramBot {
  public static final StoreScrapeBot INSTANCE = new StoreScrapeBot();

  StoreScrapeBot() {
    super(ScoreScrapeBotTelegramClient.INSTANCE, true, ScoreScrapeBotUsernameSupplier.INSTANCE);
    register(AddGroupCommand.INSTANCE);
  }

  @Override
  public void processNonCommandUpdate(Update update) {
    try {
      var sendMessage =
          SendMessage.builder()
              .chatId(update.getMessage().getChatId())
              .text("Invalid command")
              .build();
      telegramClient.execute(sendMessage);
    } catch (TelegramApiException e) {
      log.error("processNonCommandUpdate error", e);
    }
  }
}
