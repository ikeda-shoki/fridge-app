package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * リフレッシュトークン。{@code tokenHash} には生トークンではなく SHA-256 ハッシュを保持する。
 *
 * <p>失効（ログアウト・ローテーション・退会）は {@code revokedAt} で表す。行は消さないため、有効判定は必ず {@link #isValid()} を通すこと。
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends AbstractAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  protected RefreshToken() {}

  public RefreshToken(User user, String tokenHash, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  /** 未失効かつ有効期限内であれば true。 */
  public boolean isValid() {
    return revokedAt == null && expiresAt.isAfter(Instant.now());
  }

  /** 失効させる。以降このトークンでは更新できない。 */
  public void revoke() {
    this.revokedAt = Instant.now();
  }
}
