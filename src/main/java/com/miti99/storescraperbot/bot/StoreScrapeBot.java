package com.miti99.storescraperbot.bot;

import static com.miti99.storescraperbot.constant.Constant.VIETNAM_ZONE_ID;
import static com.miti99.storescraperbot.constant.Constant.WEEKENDS;

import com.miti99.storescraperbot.api.apple.AppStoreScraper;
import com.miti99.storescraperbot.api.google.GooglePlayScraper;
import com.miti99.storescraperbot.bot.command.AddAppleAppCommand;
import com.miti99.storescraperbot.bot.command.AddGoogleAppCommand;
import com.miti99.storescraperbot.bot.command.AddGroupCommand;
import com.miti99.storescraperbot.bot.command.CheckAppCommand;
import com.miti99.storescraperbot.bot.command.CheckAppScoreCommand;
import com.miti99.storescraperbot.bot.command.DeleteAppleAppCommand;
import com.miti99.storescraperbot.bot.command.DeleteGroupCommand;
import com.miti99.storescraperbot.bot.command.InfoCommand;
import com.miti99.storescraperbot.bot.command.ListAppCommand;
import com.miti99.storescraperbot.bot.command.ListGroupCommand;
import com.miti99.storescraperbot.bot.command.RawAppleAppCommand;
import com.miti99.storescraperbot.bot.command.RawGoogleAppCommand;
import com.miti99.storescraperbot.bot.entity.NonUpdatedApp;
import com.miti99.storescraperbot.bot.table.Table;
import com.miti99.storescraperbot.constant.Constant;
import com.miti99.storescraperbot.repository.AdminRepository;
import com.miti99.storescraperbot.repository.GroupRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.extensions.bots.commandbot.CommandLongPollingTelegramBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Log4j2
public class StoreScrapeBot extends CommandLongPollingTelegramBot {
  public static final StoreScrapeBot INSTANCE = new StoreScrapeBot();

  StoreScrapeBot() {
    super(StoreScrapeBotTelegramClient.INSTANCE, true, StoreScrapeBotUsernameSupplier.INSTANCE);
    register(InfoCommand.INSTANCE);

    register(RawAppleAppCommand.INSTANCE);
    register(RawGoogleAppCommand.INSTANCE);

    register(AddGroupCommand.INSTANCE);
    register(DeleteGroupCommand.INSTANCE);
    register(ListGroupCommand.INSTANCE);

    register(AddAppleAppCommand.INSTANCE);
    register(DeleteAppleAppCommand.INSTANCE);
    register(AddGoogleAppCommand.INSTANCE);
    register(DeleteAppleAppCommand.INSTANCE);
    register(ListAppCommand.INSTANCE);
    register(CheckAppCommand.INSTANCE);
    register(CheckAppScoreCommand.INSTANCE);
    setMyCommands();
  }

  private void setMyCommands() {
    try {
      var commands =
          getRegisteredCommands().stream()
              .map(cmd -> new BotCommand(cmd.getCommandIdentifier(), cmd.getDescription()))
              .toList();
      var setMyCommands = SetMyCommands.builder().commands(commands).build();
      StoreScrapeBotTelegramClient.INSTANCE.execute(setMyCommands);
    } catch (TelegramApiException e) {
      log.error("register error", e);
    }
  }

  @Override
  public void processNonCommandUpdate(Update update) {
    // Ignore
  }

  public void runCheckApp() {
    var admin = AdminRepository.INSTANCE.load();
    for (var groupId : admin.getGroups()) {
      checkAppForGroup(groupId);
    }
  }

  public void checkAppForGroup(long groupId) {
    var group = GroupRepository.INSTANCE.load(groupId);
    var now = LocalDate.now();

    var nonUpdatedAppleApps = new ArrayList<NonUpdatedApp>();
    for (var app : group.getAppleApps()) {
      var appId = app.appId();
      var updated = AppStoreScraper.getAppUpdated(appId, app.country());
      long days = ChronoUnit.DAYS.between(updated, now);
      if (days > Constant.NUM_DAYS_WARNING_NOT_UPDATED) {
        nonUpdatedAppleApps.add(new NonUpdatedApp(appId, updated, days));
      }
    }
    var nonUpdatedGoogleApps = new ArrayList<NonUpdatedApp>();
    for (var app : group.getGoogleApps()) {
      var appId = app.appId();
      var updated = GooglePlayScraper.getLastUpdateOfApp(appId, app.country());
      long days = ChronoUnit.DAYS.between(updated, now);
      if (days > Constant.NUM_DAYS_WARNING_NOT_UPDATED) {
        nonUpdatedGoogleApps.add(new NonUpdatedApp(appId, updated, days));
      }
    }
    int numNonUpdatedApps = nonUpdatedAppleApps.size() + nonUpdatedGoogleApps.size();
    if (numNonUpdatedApps == 0) return;

    var sb =
        new StringBuilder("You have %d app(s) need to be updated!\n".formatted(numNonUpdatedApps));
    if (!nonUpdatedAppleApps.isEmpty()) {
      sb.append("<b>%d Apple Apps:</b>\n".formatted(nonUpdatedAppleApps.size()));
      sb.append("<code>\n");
      var appleTable = new Table("#", "AppId", "Updated", "Days");
      int i = 0;
      for (var app : nonUpdatedAppleApps) {
        i++;
        var appId = app.appId();
        var updated = app.updated();
        long days = app.days();
        appleTable.addRow(i, appId, updated, days);
      }
      sb.append(appleTable);
      sb.append("</code>\n");
    }

    if (!nonUpdatedGoogleApps.isEmpty()) {
      sb.append("<b>%d Google Apps:</b>\n".formatted(nonUpdatedGoogleApps.size()));
      sb.append("<code>\n");
      var googleTable = new Table("#", "AppId", "Updated", "Days");
      int i = 0;
      for (var app : nonUpdatedGoogleApps) {
        i++;
        var appId = app.appId();
        var updated = app.updated();
        long days = app.days();
        googleTable.addRow(i, appId, updated, days);
      }
      sb.append(googleTable);
      sb.append("</code>");
    }

    var dayOfWeek = LocalDate.now(VIETNAM_ZONE_ID).getDayOfWeek();
    boolean mute = WEEKENDS.contains(dayOfWeek);
    try {
      var sendMessage =
          SendMessage.builder()
              .parseMode(ParseMode.HTML)
              .chatId(groupId)
              .disableNotification(mute)
              .text(sb.toString())
              .build();
      StoreScrapeBotTelegramClient.INSTANCE.execute(sendMessage);
    } catch (Exception e) {
      log.error("sendMessage error", e);
    }
  }
}
