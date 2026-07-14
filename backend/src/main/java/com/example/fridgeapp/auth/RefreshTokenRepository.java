package com.example.fridgeapp.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** リフレッシュトークンの永続化。 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  /** ハッシュ値でトークンを検索する。失効済み・期限切れも返るため、有効判定は呼び出し側で行うこと。 */
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  /** 対象ユーザーの未失効トークンをまとめて失効させる（退会時に使用）。 */
  @Modifying
  @Query(
      "UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt"
          + " WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
  void revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
