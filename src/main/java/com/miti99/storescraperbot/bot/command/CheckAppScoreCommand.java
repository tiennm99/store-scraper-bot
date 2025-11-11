package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.api.apple.AppStoreScraper;
import com.miti99.storescraperbot.api.google.GooglePlayScraper;
import com.miti99.storescraperbot.bot.StoreScrapeBotTelegramClient;
import com.miti99.storescraperbot.bot.table.Table;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import org.apache.commons.math3.util.Precision;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class CheckAppScoreCommand extends BaseStoreScraperBotCommand {
  public static final CheckAppScoreCommand INSTANCE = new CheckAppScoreCommand();
  public static final int SCORE_ROUNDING_SCALE = 1;

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
    var appleTable = new Table("AppId", "Score", "Ratings");
    for (var app : group.getAppleApps()) {
      var appId = app.appId();
      var country = app.country();
      double score =
          Precision.round(AppStoreScraper.getAppScore(appId, country), SCORE_ROUNDING_SCALE);
      long ratings = AppStoreScraper.getAppRatings(appId, country);
      appleTable.addRow(appId, score, ratings);
    }
    sb.append(appleTable);
    sb.append("</code>\n");

    sb.append("<b>Google Apps:</b>\n");
    sb.append("<code>\n");
    var googleTable = new Table("AppId", "Score", "Ratings");
    for (var app : group.getGoogleApps()) {
      var appId = app.appId();
      var country = app.country();
      double score =
          Precision.round(GooglePlayScraper.getAppScore(appId, country), SCORE_ROUNDING_SCALE);
      long ratings = GooglePlayScraper.getAppRatings(appId, country);
      googleTable.addRow(appId, score, ratings);
    }
    sb.append(googleTable);
    sb.append("</code>");

    StoreScrapeBotTelegramClient.INSTANCE.sendMessage(chat.getId(), sb.toString());
  }
}
