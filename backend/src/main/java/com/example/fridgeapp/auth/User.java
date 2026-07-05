package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends AbstractAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "google_sub", nullable = false, updatable = false, length = 255)
  private String googleSub;

  @Column(name = "display_name", nullable = false, length = 255)
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected User() {}

  public User(String googleSub, String displayName, String avatarUrl) {
    this.googleSub = googleSub;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
  }

  public UUID getId() {
    return id;
  }

  public String getGoogleSub() {
    return googleSub;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  /** 再ログイン時に Google プロフィール情報を最新化する。 */
  public void updateProfile(String displayName, String avatarUrl) {
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
  }

  public void markDeleted() {
    this.deletedAt = Instant.now();
  }
}
