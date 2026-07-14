package com.example.fridgeapp.auth;

import java.util.UUID;

/** ユーザー情報のレスポンス。{@code googleSub} などの内部情報は含めない。 */
public record UserResponse(UUID id, String displayName, String avatarUrl) {

  /** エンティティからレスポンスへ変換する。 */
  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getDisplayName(), user.getAvatarUrl());
  }
}
