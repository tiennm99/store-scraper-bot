package com.miti99.storescraperbot.model;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Admin extends AbstractModel<String> {
  Set<Long> groups = new HashSet<>();
}
