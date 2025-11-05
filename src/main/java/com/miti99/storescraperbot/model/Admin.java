package com.miti99.storescraperbot.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class Admin extends AbstractModel<String> {
  final List<Long> groups = new ArrayList<>();
}
