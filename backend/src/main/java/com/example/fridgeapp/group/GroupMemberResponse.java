package com.example.fridgeapp.group;

import com.example.fridgeapp.auth.User;
import java.time.Instant;
import java.util.UUID;

public record GroupMemberResponse(
    UUID userId, String displayName, String avatarUrl, GroupRole role, Instant joinedAt) {

  public static GroupMemberResponse of(GroupMember member, User user) {
    return new GroupMemberResponse(
        user.getId(),
        user.getDisplayName(),
        user.getAvatarUrl(),
        member.getRole(),
        member.getJoinedAt());
  }
}
