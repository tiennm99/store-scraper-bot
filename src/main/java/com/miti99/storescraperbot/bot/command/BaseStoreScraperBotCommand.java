package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Log4j2
public abstract class BaseStoreScraperBotCommand extends BotCommand {

  public BaseStoreScraperBotCommand(String commandIdentifier, String description) {
    super(commandIdentifier, description);
  }

  @Override
  public void execute(TelegramClient telegramClient, User user, Chat chat, String[] arguments) {
    try {
      executeCommand(telegramClient, user, chat, arguments);
    } catch (Exception e) {
      log.error("execute error", e);
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Internal server error");
    }
  }

  protected abstract void executeCommand(
      TelegramClient telegramClient, User user, Chat chat, String[] arguments);
}
