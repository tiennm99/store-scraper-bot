package com.miti99.storescraperbot.bot;

import com.miti99.storescraperbot.env.Environment;
import java.util.function.Supplier;

public class StoreScrapeBotUsernameSupplier implements Supplier<String> {
  public static final StoreScrapeBotUsernameSupplier INSTANCE =
      new StoreScrapeBotUsernameSupplier();

  @Override
  public String get() {
    return Environment.TELEGRAM_BOT_USERNAME;
  }
}
