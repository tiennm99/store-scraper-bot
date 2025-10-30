package com.miti99.storescraperbot.bot;

import com.miti99.storescraperbot.config.Config;
import java.util.function.Supplier;

public class ScoreScrapeBotUsernameSupplier implements Supplier<String> {
  public static final ScoreScrapeBotUsernameSupplier INSTANCE =
      new ScoreScrapeBotUsernameSupplier();

  @Override
  public String get() {
    return Config.TELEGRAM_BOT_USERNAME;
  }
}
