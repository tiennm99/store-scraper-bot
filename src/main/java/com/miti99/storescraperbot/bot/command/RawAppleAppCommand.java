package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.api.old.apple.AppStoreScraper;
import com.miti99.storescraperbot.api.old.apple.request.AppleAppRequest;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.repository.AdminRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Log4j2
public class RawAppleAppCommand extends BaseStoreScraperBotCommand {
  public static final RawAppleAppCommand INSTANCE = new RawAppleAppCommand();

  RawAppleAppCommand() {
    super(
        "rawappleapp",
        "<id/appId> [country]. Lấy raw response khi request lên service. id: <i>iTunes 'trackId'</i>, appId: <i>iTunes 'bundleId'</i>. Một số app cần country để hoạt động đúng, country mặc định là 'vn'");
  }

  @Override
  @SneakyThrows
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
    String response = "";
    try {
      try {
        id = Long.parseLong(appId);
      } catch (Exception e) {
        // Input không phải id, bỏ qua
      }
      if (id != -1) {
        response = AppStoreScraper.rawApp(new AppleAppRequest(id, country));
      } else {
        response = AppStoreScraper.rawApp(new AppleAppRequest(appId, country));
      }
    } catch (Exception e) {
      log.error("request app error for appId: '{}', id: '{}'", appId, id, e);
      StoreScrapeBotTelegramClient.INSTANCE.sendMessage(
          chat.getId(), "Error when request app info");
      return;
    }

    if (response == null) response = "";

    var inputStream = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));

    var file = new InputFile(inputStream, "%s.json".formatted(appId));

    var sendDocument =
        SendDocument.builder()
            .chatId(chat.getId())
            .document(file)
            // .caption("raw")
            .build();

    StoreScrapeBotTelegramClient.INSTANCE.execute(sendDocument);
  }
}
