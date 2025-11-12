package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.env.Environment;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class AddGroupCommand extends BaseStoreScraperBotCommand {
  public static final AddGroupCommand INSTANCE = new AddGroupCommand();

  AddGroupCommand() {
    super("addgroup", "<groupId>. Thêm group vào list group cho phép sử dụng bot");
  }

  @Override
  protected void executeCommand(
      TelegramClient telegramClient, User user, Chat chat, String[] arguments) {
    if (!Environment.ADMIN_IDS.contains(user.getId())) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "You are not admin");
      return;
    }

    if (arguments.length > 1) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Invalid arguments");
      return;
    }

    long groupId;
    if (arguments.length == 1) {
      try {
        groupId = Long.parseLong(arguments[0]);
      } catch (NumberFormatException e) {
        StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Invalid groupId");
        return;
      }
    } else {
      groupId = chat.getId();
    }

    var admin = AdminRepository.INSTANCE.load();
    if (admin.groups().contains(groupId)) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Group is already added");
      return;
    }

    GroupRepository.INSTANCE.init(groupId);
    admin.groups().add(groupId);
    AdminRepository.INSTANCE.save(admin);
    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Group added successfully");
  }
}
