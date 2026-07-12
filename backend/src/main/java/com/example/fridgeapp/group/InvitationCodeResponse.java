package com.example.fridgeapp.group;

import java.time.Instant;

public record InvitationCodeResponse(String code, Instant expiresAt) {

  public static InvitationCodeResponse from(InvitationCode invitationCode) {
    return new InvitationCodeResponse(invitationCode.getCode(), invitationCode.getExpiresAt());
  }
}
