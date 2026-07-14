package com.example.fridgeapp.group;

import com.example.fridgeapp.auth.User;
import java.time.Instant;
import java.util.UUID;

/** グループメンバーのレスポンス。所属情報とユーザーの表示情報をまとめて返す。 */
public record GroupMemberResponse(
    UUID userId, String displayName, String avatarUrl, GroupRole role, Instant joinedAt) {

  /** 所属（{@link GroupMember}）とユーザーを組み合わせてレスポンスへ変換する。 */
  public static GroupMemberResponse of(GroupMember member, User user) {
    return new GroupMemberResponse(
        user.getId(),
        user.getDisplayName(),
        user.getAvatarUrl(),
        member.getRole(),
        member.getJoinedAt());
  }
}
