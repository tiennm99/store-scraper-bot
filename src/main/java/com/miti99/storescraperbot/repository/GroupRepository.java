package com.miti99.storescraperbot.repository;

import com.miti99.storescraperbot.model.Group;

public class GroupRepository extends AbstractRepository<Long, Group> {
  public static final GroupRepository INSTANCE = new GroupRepository();
}
