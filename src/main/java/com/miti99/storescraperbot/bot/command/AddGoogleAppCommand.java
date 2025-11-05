package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class AddGoogleAppCommand extends BaseStoreScraperBotCommand {
  public static final AddGoogleAppCommand INSTANCE = new AddGoogleAppCommand();

  AddGoogleAppCommand() {
    super("addgoogle", "<appId>. Thêm Google app vào danh sách theo dõi của nhóm");
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

    if (arguments.length != 1) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Invalid arguments");
      return;
    }

    var appId = arguments[0];
    long groupId = chat.getId();
    var group = GroupRepository.INSTANCE.load(groupId);

    if (group.getGoogleApps().contains(appId)) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
          chat.getId(), "Google app is already added");
      return;
    }

    group.getGoogleApps().add(appId);
    GroupRepository.INSTANCE.save(groupId, group);
    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
        chat.getId(), "Google app added successfully");
  }
}
