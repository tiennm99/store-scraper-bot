package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.api.apple.AppStoreScraper;
import com.miti99.storescraperbot.api.apple.request.AppleAppRequest;
import com.miti99.storescraperbot.api.apple.response.AppleAppResponse;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.model.entity.AppleAppInfo;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Log4j2
public class AddAppleAppCommand extends BaseStoreScraperBotCommand {
  public static final AddAppleAppCommand INSTANCE = new AddAppleAppCommand();

  AddAppleAppCommand() {
    super(
        "addapple",
        "<id/appId> [country]. Thêm Apple app vào danh sách theo dõi của nhóm. id: <i>iTunes 'trackId'</i>, appId: <i>iTunes 'bundleId'</i>. Một số app cần country để hoạt động đúng, country mặc định là 'vn'");
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
    long id = -1;
    var country = arguments.length == 2 ? arguments[1] : "vn";
    AppleAppResponse response = null;
    try {
      try {
        id = Long.parseLong(appId);
      } catch (Exception e) {
        // Input không phải id, bỏ qua
      }
      if (id != -1) {
        response = AppStoreScraper.app(new AppleAppRequest(id, country));
      } else {
        response = AppStoreScraper.app(new AppleAppRequest(appId, country));
      }
    } catch (Exception e) {
      log.error("request app error for appId: '{}', id: '{}'", appId, id, e);
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
          chat.getId(), "Error when request app info");
      return;
    }
    appId = response.appId();

    long groupId = chat.getId();
    var group = GroupRepository.INSTANCE.load(groupId);

    var finalAppId = appId;
    if (group.getAppleApps().stream().anyMatch(app -> finalAppId.equals(app.appId()))) {
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
          chat.getId(), "Apple app <code>%s</code> is already added".formatted(appId));
      return;
    }

    group.getAppleApps().add(new AppleAppInfo(appId, country));
    GroupRepository.INSTANCE.save(groupId, group);
    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
        chat.getId(),
        "Apple app <code>%s</code>, country <b>%s</b> added successfully"
            .formatted(appId, country));
  }
}
