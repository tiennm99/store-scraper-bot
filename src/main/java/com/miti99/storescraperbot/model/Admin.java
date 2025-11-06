package com.miti99.storescraperbot.model;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class Admin extends AbstractModel<String> {
  final Set<Long> groups = new HashSet<>();
}
