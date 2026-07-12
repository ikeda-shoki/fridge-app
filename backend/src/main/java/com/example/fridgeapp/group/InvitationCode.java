package com.example.fridgeapp.group;

import com.example.fridgeapp.common.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "invitation_codes")
public class InvitationCode extends AbstractAuditableEntity {

  private static final int FAILURE_LOCK_THRESHOLD = 10;
  private static final Duration LOCK_DURATION = Duration.ofHours(3);

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "code", nullable = false, updatable = false, columnDefinition = "char(6)")
  private String code;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_by")
  private UUID usedBy;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  protected InvitationCode() {}

  public InvitationCode(UUID groupId, String code, Instant expiresAt) {
    this.groupId = groupId;
    this.code = code;
    this.expiresAt = expiresAt;
    this.failedAttempts = 0;
  }

  public UUID getId() {
    return id;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public String getCode() {
    return code;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public UUID getUsedBy() {
    return usedBy;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public boolean isExpired(Instant now) {
    return now.isAfter(expiresAt);
  }

  public boolean isLocked(Instant now) {
    return lockedUntil != null && now.isBefore(lockedUntil);
  }

  /** 即時失効させる（新しいコード発行時に既存の有効コードを無効化するために使用）。 */
  public void expireNow() {
    this.expiresAt = Instant.now();
  }

  public void recordFailedAttempt(Instant now) {
    this.failedAttempts++;
    if (this.failedAttempts >= FAILURE_LOCK_THRESHOLD) {
      this.lockedUntil = now.plus(LOCK_DURATION);
    }
  }

  public void markUsed(UUID userId, Instant now) {
    this.usedBy = userId;
    this.usedAt = now;
  }
}
