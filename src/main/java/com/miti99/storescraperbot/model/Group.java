package com.miti99.storescraperbot.model;

import com.miti99.storescraperbot.type.AppType;
import java.util.List;
import lombok.Data;

@Data
public class Group {
  List<App> apps;

  public static class App {
    String appId;
    AppType type;
  }
}
