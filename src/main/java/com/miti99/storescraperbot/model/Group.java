package com.miti99.storescraperbot.model;

import com.miti99.storescraperbot.model.entity.AppleAppInfo;
import com.miti99.storescraperbot.model.entity.GoogleAppInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Group extends AbstractModel<Long> {
  List<AppleAppInfo> appleApps = new ArrayList<>();
  List<GoogleAppInfo> googleApps = new ArrayList<>();
}
