package com.example.fridgeapp.foodmaster;

import com.example.fridgeapp.common.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "food_master")
public class FoodMaster extends AbstractAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "name_kana", nullable = false, length = 100)
  private String nameKana;

  @Column(name = "default_category", length = 50)
  private String defaultCategory;

  @Column(name = "default_unit", length = 20)
  private String defaultUnit;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  protected FoodMaster() {}

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getNameKana() {
    return nameKana;
  }

  public String getDefaultCategory() {
    return defaultCategory;
  }

  public String getDefaultUnit() {
    return defaultUnit;
  }

  public boolean isActive() {
    return active;
  }
}
