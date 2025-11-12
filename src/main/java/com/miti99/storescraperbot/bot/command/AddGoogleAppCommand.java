package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.api.old.google.GooglePlayScraper;
import com.miti99.storescraperbot.api.old.google.request.GoogleAppRequest;
import com.miti99.storescraperbot.api.old.google.response.GoogleAppResponse;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.model.entity.GoogleAppInfo;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Log4j2
public class AddGoogleAppCommand extends BaseStoreScraperBotCommand {
  public static final AddGoogleAppCommand INSTANCE = new AddGoogleAppCommand();

  AddGoogleAppCommand() {
    super(
        "addgoogle",
        "<appId> [country]. Thêm Google app vào danh sách theo dõi của nhóm. Một số app cần country để hoạt động đúng, country mặc định là 'vn'");
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

    if (arguments.length < 1 || arguments.length > 2) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), "Invalid arguments");
      return;
    }

    var appId = arguments[0];
    var country = arguments.length == 2 ? arguments[1] : "vn";
    GoogleAppResponse response = null;
    try {
      response = GooglePlayScraper.app(new GoogleAppRequest(appId, country));
    } catch (Exception e) {
      log.error("request app error for appId: '{}'", appId, e);
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
          chat.getId(), "Error when request app info");
      return;
    }

    long groupId = chat.getId();
    var group = GroupRepository.INSTANCE.load(groupId);

    if (group.getGoogleApps().stream().anyMatch(app -> appId.equals(app.appId()))) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
          chat.getId(), "Google app <code>%s</code> is already added".formatted(appId));
      return;
    }

    group.getGoogleApps().add(new GoogleAppInfo(appId, country));
    GroupRepository.INSTANCE.save(groupId, group);
    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
        chat.getId(),
        "Google app <code>%s</code>, country <b>%s</b> added successfully"
            .formatted(appId, country));
  }
}
