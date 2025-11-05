package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class InfoCommand extends BaseStoreScraperBotCommand {
  public static final InfoCommand INSTANCE = new InfoCommand();

  InfoCommand() {
    super("info", "Lấy thông tin của nhóm (chatId, threadId,...)");
  }

  @Override
  protected void executeCommand(
      TelegramClient telegramClient, User user, Chat chat, String[] arguments) {
    if (arguments.length != 0) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Invalid arguments");
      return;
    }

    long groupId = chat.getId();

    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Id của nhóm là <code>%s</code>\n".formatted(groupId));
  }
}
