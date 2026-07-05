package com.example.fridgeapp.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/** 作成・更新の4監査列を持つエンティティの基底クラス（consumption_events を除く全テーブルで使用）。 */
@MappedSuperclass
public abstract class AbstractAuditableEntity extends AbstractCreatedOnlyEntity {

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @LastModifiedBy
  @Column(name = "updated_by")
  private UUID updatedBy;

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }
}
