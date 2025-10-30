package com.miti99.storescraperbot.bot;

import com.miti99.storescraperbot.config.Config;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;

public class ScoreScrapeBotTelegramClient extends OkHttpTelegramClient {
  public static final ScoreScrapeBotTelegramClient INSTANCE = new ScoreScrapeBotTelegramClient();

  public ScoreScrapeBotTelegramClient() {
    super(Config.TELEGRAM_BOT_TOKEN);
  }
}
