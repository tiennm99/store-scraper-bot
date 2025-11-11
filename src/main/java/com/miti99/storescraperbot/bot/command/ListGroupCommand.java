package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.env.Environment;
import com.miti99.storescraperbot.repository.AdminRepository;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class ListGroupCommand extends BaseStoreScraperBotCommand {
  public static final ListGroupCommand INSTANCE = new ListGroupCommand();

  ListGroupCommand() {
    super("listgroup", "Lấy danh sách group được phép sử dụng bot hiện tại");
  }

  @Override
  protected void executeCommand(
      TelegramClient telegramClient, User user, Chat chat, String[] arguments) {
    if (!Environment.ADMIN_IDS.contains(user.getId())) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "You are not admin");
      return;
    }

    if (arguments.length != 0) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Invalid arguments");
      return;
    }

    var admin = AdminRepository.INSTANCE.load();
    var groups = admin.getGroups();
    var sb = new StringBuilder();
    sb.append("<b>Groups:</b>\n");
    for (var groupId : groups) {
      sb.append("- <code>%s</code>\n".formatted(groupId));
    }
    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), sb.toString());
  }
}
