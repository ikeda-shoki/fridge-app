package com.example.fridgeapp.group;

import java.time.Instant;

/** 発行した招待コードのレスポンス。失敗回数などの内部状態は含めない。 */
public record InvitationCodeResponse(String code, Instant expiresAt) {

  /** エンティティからレスポンスへ変換する。 */
  public static InvitationCodeResponse from(InvitationCode invitationCode) {
    return new InvitationCodeResponse(invitationCode.getCode(), invitationCode.getExpiresAt());
  }
}
