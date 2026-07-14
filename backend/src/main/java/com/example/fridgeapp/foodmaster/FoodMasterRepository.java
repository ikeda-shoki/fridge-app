package com.example.fridgeapp.foodmaster;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 食材マスタの永続化。 */
public interface FoodMasterRepository extends JpaRepository<FoodMaster, UUID> {

  /**
   * 有効な食材のうち、よみがなが前方一致するものをよみがな昇順で返す。
   *
   * @param nameKanaPrefix よみがなの前方一致パターン。LIKE のワイルドカード（{@code %} {@code _} {@code \}）は呼び出し側で
   *     エスケープ済みであること
   */
  @Query(
      """
      SELECT f FROM FoodMaster f
      WHERE f.active = true
        AND f.nameKana LIKE CONCAT(:nameKanaPrefix, '%') ESCAPE '\\'
      ORDER BY f.nameKana ASC
      """)
  List<FoodMaster> findActiveByNameKanaPrefix(
      @Param("nameKanaPrefix") String nameKanaPrefix, Limit limit);
}
