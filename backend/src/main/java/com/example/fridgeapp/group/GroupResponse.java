package com.example.fridgeapp.group;

import java.util.UUID;

public record GroupResponse(UUID id, String name) {

  public static GroupResponse from(Group group) {
    return new GroupResponse(group.getId(), group.getName());
  }
}
