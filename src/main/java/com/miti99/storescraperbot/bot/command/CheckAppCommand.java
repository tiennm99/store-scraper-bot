package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.api.old.apple.AppStoreScraper;
import com.miti99.storescraperbot.api.old.google.GooglePlayScraper;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.bot.table.Table;
import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class CheckAppCommand extends BaseStoreScraperBotCommand {
  public static final CheckAppCommand INSTANCE = new CheckAppCommand();

  CheckAppCommand() {
    super("checkapp", "Kiểm tra cập nhật các app");
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
    var now = LocalDate.now();

    var sb = new StringBuilder();
    sb.append("<b>Apple Apps:</b>\n");
    sb.append("<code>\n");
    var appleTable = new Table("AppId", "Updated", "Days", "OK");
    for (var app : group.appleApps()) {
      var appId = app.appId();
      var updated = AppStoreScraper.getAppUpdated(appId, app.country());
      long days = ChronoUnit.DAYS.between(updated, now);
      boolean passed = days <= Constant.NUM_DAYS_WARNING_NOT_UPDATED;
      var icon = passed ? "✅" : "❌";
      appleTable.addRow(appId, updated, days, icon);
    }
    sb.append(appleTable);
    sb.append("</code>\n");

    sb.append("<b>Google Apps:</b>\n");
    sb.append("<code>\n");
    var googleTable = new Table("AppId", "Updated", "Days", "OK");
    for (var app : group.googleApps()) {
      var appId = app.appId();
      var updated = GooglePlayScraper.getLastUpdateOfApp(appId, app.country());
      long days = ChronoUnit.DAYS.between(updated, now);
      boolean passed = days <= Constant.NUM_DAYS_WARNING_NOT_UPDATED;
      var icon = passed ? "✅" : "❌";
      googleTable.addRow(appId, updated, days, icon);
    }
    sb.append(googleTable);
    sb.append("</code>");

    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), sb.toString());
  }
}
