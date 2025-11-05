package com.miti99.storescraperbot.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Group extends AbstractModel<Long> {
  List<String> appleApps = new ArrayList<>();
  List<String> googleApps = new ArrayList<>();
}
