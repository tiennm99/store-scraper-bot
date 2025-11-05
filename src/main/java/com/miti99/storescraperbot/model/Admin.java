package com.miti99.storescraperbot.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Admin extends AbstractModel<String> {
  List<Long> groups = new ArrayList<>();

  public Admin(String key) {
    super(key);
  }
}
