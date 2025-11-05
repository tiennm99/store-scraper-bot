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
    super("listapp", "<appId>. Thêm Google app vào danh sách theo dõi của nhóm");
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
    for (var appId : group.getAppleApps()) {
      sb.append("- <code>%s</code>\n".formatted(appId));
    }
    sb.append("\n<b>Google Apps:</b>\n");
    for (var appId : group.getGoogleApps()) {
      sb.append("- <code>%s</code>\n".formatted(appId));
    }

    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), sb.toString());
  }
}
