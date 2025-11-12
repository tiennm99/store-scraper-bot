package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.bot.table.Table;
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
    if (!admin.groups().contains(chat.getId())) {
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
    var appleTable = new Table("#", "AppId", "Country");
    int i = 0;
    for (var app : group.appleApps()) {
      i++;
      appleTable.addRow(i, app.appId(), app.country());
    }
    sb.append(appleTable);
    sb.append("</code>\n");

    sb.append("<b>Google Apps:</b>\n");
    sb.append("<code>\n");
    var googleTable = new Table("#", "AppId", "Country");
    i = 0;
    for (var app : group.googleApps()) {
      i++;
      googleTable.addRow(i, app.appId(), app.country());
    }
    sb.append(googleTable);
    sb.append("</code>");

    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), sb.toString());
  }
}
