package com.miti99.storescraperbot.bot.command;

import com.miti99.storescraperbot.config.Config;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class AddGroupCommand extends BotCommand {
  public static final AddGroupCommand INSTANCE = new AddGroupCommand();

  AddGroupCommand() {
    super("addgroup", "Thêm group vào list group cho phép sử dụng bot");
  }

  @Override
  public void execute(TelegramClient telegramClient, User user, Chat chat, String[] arguments) {
    if (!Config.ADMIN_IDS.contains(user.getId()) ) {
      return;
    }
    // TODO
  }
}
