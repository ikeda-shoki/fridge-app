package com.example.fridgeapp.auth;

import java.util.UUID;

public record UserResponse(UUID id, String displayName, String avatarUrl) {

  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getDisplayName(), user.getAvatarUrl());
  }
}
