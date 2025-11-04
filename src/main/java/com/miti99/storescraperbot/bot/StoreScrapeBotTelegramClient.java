package com.miti99.storescraperbot.bot;

import com.miti99.storescraperbot.config.Config;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Log4j2
public class StoreScrapeBotTelegramClient extends OkHttpTelegramClient {
  public static final StoreScrapeBotTelegramClient INSTANCE = new StoreScrapeBotTelegramClient();

  public StoreScrapeBotTelegramClient() {
    super(Config.TELEGRAM_BOT_TOKEN);
  }

  public void sendMessage(long chatId, String text) {
    try {
      var sendMessage =
          SendMessage.builder().parseMode(ParseMode.HTML).chatId(chatId).text(text).build();
      execute(sendMessage);
    } catch (Exception e) {
      log.error("sendMessage error", e);
    }
  }

  public void sendMessage(long chatId, int threadId, String text) {
    try {
      var sendMessage =
          SendMessage.builder()
              .parseMode(ParseMode.HTML)
              .chatId(chatId)
              .messageThreadId(threadId)
              .text(text)
              .build();
      execute(sendMessage);
    } catch (Exception e) {
      log.error("sendMessage error", e);
    }
  }
}
