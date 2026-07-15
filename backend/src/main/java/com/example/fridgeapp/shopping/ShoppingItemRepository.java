package com.example.fridgeapp.shopping;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 買い物リストアイテムの永続化。 */
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, UUID> {

  /** グループの買い物リストを登録順に返す。 */
  List<ShoppingItem> findByGroupIdOrderByCreatedAtAsc(UUID groupId);

  /** チェック済みアイテムを一括削除する（SHP-05）。 */
  @Modifying
  @Query("DELETE FROM ShoppingItem s WHERE s.groupId = :groupId AND s.checked = true")
  void deleteCheckedByGroupId(@Param("groupId") UUID groupId);
}
