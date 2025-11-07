package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class ListAppCommand extends BaseStoreScraperBotCommand {
  public static final ListAppCommand INSTANCE = new ListAppCommand();

  ListAppCommand() {
    super("listapp", "<appId>. Lấy danh sách app đang theo dõi của nhóm");
  }

  @Override
  protected void executeCommand(
      TelegramClient telegramClient, User user, Chat chat, String[] arguments) {
    var admin = AdminRepository.INSTANCE.load();
    if (!admin.getGroups().contains(chat.getId())) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
          chat.getId(), "Group is not allowed to use bot");
      return;
    }

    if (arguments.length != 0) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Invalid arguments");
      return;
    }

    long groupId = chat.getId();
    var group = GroupRepository.INSTANCE.load(groupId);

    var sb = new StringBuilder();
    sb.append("<b>Apple Apps:</b>\n");
    sb.append("<code>\n");
    sb.append("%-2s | %-20s | %-7s\n".formatted("#", "AppId", "Country"));
    sb.append("-".repeat(25));
    sb.append("\n");
    int i = 0;
    for (var app : group.getAppleApps()) {
      i++;
      sb.append("%-2s | %-20s | %-7s\n".formatted(i, app.appId(), app.country()));
    }
    sb.append("</code>\n");
    sb.append("\n");

    sb.append("\n<b>Google Apps:</b>\n");
    sb.append("<code>\n");
    sb.append("%-2s | %-20s | %-7s\n".formatted("#", "AppId", "Country"));
    sb.append("-".repeat(35));
    sb.append("\n");
    i = 0;
    for (var app : group.getGoogleApps()) {
      i++;
      sb.append("%-2s | %-20s | %-7s\n".formatted(i, app.appId(), app.country()));
    }
    sb.append("</code>\n");
    sb.append("\n");

    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), sb.toString());
  }
}
