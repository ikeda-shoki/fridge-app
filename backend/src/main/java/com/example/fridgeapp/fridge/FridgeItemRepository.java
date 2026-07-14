package com.example.fridgeapp.fridge;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 冷蔵庫アイテムの永続化。 */
public interface FridgeItemRepository extends JpaRepository<FridgeItem, UUID> {

  /**
   * グループの ACTIVE な冷蔵庫アイテムを、賞味期限が近い順（期限なしは末尾）に返す。
   *
   * <p>絞り込みの有無は {@code anyCategory} と空文字の {@code keyword} で表す。{@code :param IS NULL} で分岐すると、null
   * バインド時に PostgreSQL がパラメータの型を決められず（bytea と推論され）エラーになるため。
   *
   * @param anyCategory true の場合はカテゴリで絞り込まない（このとき {@code category} は参照されない）
   * @param keyword 空文字の場合は名前で絞り込まない。LIKE のワイルドカードはエスケープ済みであること
   */
  @Query(
      """
      SELECT i FROM FridgeItem i
      WHERE i.groupId = :groupId
        AND i.status = com.example.fridgeapp.fridge.FridgeItemStatus.ACTIVE
        AND (:anyCategory = TRUE OR i.category = :category)
        AND i.displayName LIKE CONCAT('%', :keyword, '%') ESCAPE '\\'
      ORDER BY i.expiresAt ASC NULLS LAST, i.createdAt DESC
      """)
  List<FridgeItem> searchActiveByGroupId(
      @Param("groupId") UUID groupId,
      @Param("anyCategory") boolean anyCategory,
      @Param("category") FridgeItemCategory category,
      @Param("keyword") String keyword);
}
