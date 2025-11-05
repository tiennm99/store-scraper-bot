package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.api.apple.AppStoreScraper;
import com.miti99.storescraperbot.api.google.GooglePlayScraper;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    var now = LocalDate.now();

    var sb = new StringBuilder();
    sb.append("<b>Apple Apps:</b>\n");
    sb.append("<code>\n");
    sb.append("%-20s | %-10s | %-4s | %-2s\n".formatted("AppId", "Updated", "Days", "OK"));
    sb.append("-".repeat(46));
    sb.append("\n");
    for (var appId : group.getAppleApps()) {
      var updated = AppStoreScraper.getLastUpdateOfApp(appId);
      long days = ChronoUnit.DAYS.between(updated, now);
      boolean passed = days <= Constant.NUM_DAYS_WARNING_NOT_UPDATED;
      sb.append(
          "%-20s | %-10s | %-4s | %-2s\n".formatted(appId, updated, days, passed ? "✅" : "❌"));
    }
    sb.append("</code>\n");
    sb.append("\n");
    sb.append("<b>Google Apps:</b>\n");
    sb.append("<code>\n");
    sb.append("%-20s | %-10s | %-4s | %-2s\n".formatted("AppId", "Updated", "Date", "OK"));
    sb.append("-".repeat(46));
    sb.append("\n");
    for (var appId : group.getGoogleApps()) {
      var updated = GooglePlayScraper.getLastUpdateOfApp(appId);
      long days = ChronoUnit.DAYS.between(updated, now);
      boolean passed = days <= Constant.NUM_DAYS_WARNING_NOT_UPDATED;
      sb.append(
          "%-20s | %-10s | %-4s | %-2s\n".formatted(appId, updated, days, passed ? "✅" : "❌"));
    }
    sb.append("</code>");
    sb.append("\n");

    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), sb.toString());
  }
}
