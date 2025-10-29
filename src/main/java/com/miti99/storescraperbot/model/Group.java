package com.miti99.storescraperbot.model;

import com.miti99.storescraperbot.type.AppType;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Group extends AbstractModel<Long> {
  List<App> apps;

  public static class App {
    String appId;
    AppType type;
  }
}
