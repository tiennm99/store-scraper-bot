package com.miti99.storescraperbot.bot;

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
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.extensions.bots.commandbot.CommandLongPollingTelegramBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Log4j2
public class StoreScrapeBot extends CommandLongPollingTelegramBot {
  public static final StoreScrapeBot INSTANCE = new StoreScrapeBot();

  StoreScrapeBot() {
    super(StoreScrapeBotTelegramClient.INSTANCE, true, StoreScrapeBotUsernameSupplier.INSTANCE);
    register(InfoCommand.INSTANCE);

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
}
