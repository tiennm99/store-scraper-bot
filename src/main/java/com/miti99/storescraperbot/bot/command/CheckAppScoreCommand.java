package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.api.apple.AppStoreScraper;
import com.miti99.storescraperbot.api.google.GooglePlayScraper;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import java.time.temporal.ChronoUnit;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class CheckAppScoreCommand extends BaseStoreScraperBotCommand {
  public static final CheckAppScoreCommand INSTANCE = new CheckAppScoreCommand();

  CheckAppScoreCommand() {
    super("checkappscore", "Kiểm tra điểm đánh giá các app (sao)");
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
    sb.append("<code>\n");
    sb.append("%-20s | %-10s | %-10s\n".formatted("AppId", "Score", "Reviews"));
    sb.append("-".repeat(32));
    sb.append("\n");
    for (var appId : group.getAppleApps()) {
      double score = AppStoreScraper.getAppScore(appId);
      long reviews = AppStoreScraper.getAppReviews(appId);
      sb.append("%-20s | %-10s | %-10s\n".formatted(appId, score, reviews));
    }
    sb.append("</code>\n");
    sb.append("\n");
    sb.append("<b>Google Apps:</b>\n");
    sb.append("<code>\n");
    sb.append("%-20s | %-10s | %-10s\n".formatted("AppId", "Score", "Ratings"));
    sb.append("-".repeat(46));
    sb.append("\n");
    for (var appId : group.getGoogleApps()) {
      double score = GooglePlayScraper.getAppScore(appId);
      long ratings = GooglePlayScraper.getAppRatings(appId);
      sb.append("%-20s | %-10s | %-10s\n".formatted(appId, score, ratings));
    }
    sb.append("</code>");
    sb.append("\n");

    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), sb.toString());
  }
}
