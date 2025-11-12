package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class DeleteGoogleAppCommand extends BaseStoreScraperBotCommand {
  public static final DeleteGoogleAppCommand INSTANCE = new DeleteGoogleAppCommand();

  DeleteGoogleAppCommand() {
    super("delgoogle", "<appId>. Xoá Google app khỏi danh sách theo dõi của nhóm");
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

    if (arguments.length != 1) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Invalid arguments");
      return;
    }

    var appId = arguments[0];
    long groupId = chat.getId();
    var group = GroupRepository.INSTANCE.load(groupId);

    if (group.googleApps().stream().noneMatch(app -> appId.equals(app.appId()))) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Google app is not added");
      return;
    }

    group.googleApps().removeIf(app -> appId.equals(app.appId()));
    GroupRepository.INSTANCE.save(groupId, group);
    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
        chat.getId(), "Google app deleted successfully");
  }
}
