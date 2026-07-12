package com.example.fridgeapp.fridge;

import com.example.fridgeapp.common.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** ステップ6時点では画像アップロード/削除に必要な列のみをマッピングする最小限のエンティティ。 冷蔵庫アイテムの登録・編集・一覧等のフル CRUD はステップ9で追加する。 */
@Entity
@Table(name = "fridge_items")
public class FridgeItem extends AbstractAuditableEntity {

  @Id private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FridgeItemStatus status;

  @Column(name = "image_path")
  private String imagePath;

  protected FridgeItem() {}

  public UUID getId() {
    return id;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public FridgeItemStatus getStatus() {
    return status;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void replaceImage(String newImagePath) {
    this.imagePath = newImagePath;
  }

  public void clearImage() {
    this.imagePath = null;
  }
}
