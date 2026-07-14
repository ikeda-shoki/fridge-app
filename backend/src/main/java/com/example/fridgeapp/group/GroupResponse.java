package com.example.fridgeapp.group;

import java.util.UUID;

/** グループのレスポンス。 */
public record GroupResponse(UUID id, String name) {

  /** エンティティからレスポンスへ変換する。 */
  public static GroupResponse from(Group group) {
    return new GroupResponse(group.getId(), group.getName());
  }
}
