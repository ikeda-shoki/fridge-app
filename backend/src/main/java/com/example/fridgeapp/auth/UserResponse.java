package com.example.fridgeapp.auth;

import com.example.fridgeapp.group.GroupResponse;
import java.util.List;
import java.util.UUID;

/**
 * ユーザー情報のレスポンス。{@code googleSub} などの内部情報は含めない。
 *
 * <p>{@code groups} は所属する家族グループの一覧（GRP-00: グループ未所属かどうかの判定にフロントエンドが使う）。
 */
public record UserResponse(
    UUID id, String displayName, String avatarUrl, List<GroupResponse> groups) {

  /** エンティティからレスポンスへ変換する。 */
  public static UserResponse from(User user, List<GroupResponse> groups) {
    return new UserResponse(user.getId(), user.getDisplayName(), user.getAvatarUrl(), groups);
  }
}
