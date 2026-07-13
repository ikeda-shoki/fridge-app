package com.example.fridgeapp.group;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationCodeRepository extends JpaRepository<InvitationCode, UUID> {

  /** グループの未使用の招待コード（1グループにつき最大1件）。 */
  @Query(
      """
      SELECT c FROM InvitationCode c
      WHERE c.groupId = :groupId
        AND c.usedAt IS NULL
      """)
  Optional<InvitationCode> findUnusedByGroupId(@Param("groupId") UUID groupId);

  /**
   * コードに一致する招待コードのうち、最も新しいもの。
   *
   * <p>コードは使い回されるため、同じ値の過去コードが残り得る。呼び出し側は {@code Limit.of(1)} を渡す。
   */
  @Query(
      """
      SELECT c FROM InvitationCode c
      WHERE c.code = :code
      ORDER BY c.createdAt DESC
      """)
  Optional<InvitationCode> findLatestByCode(@Param("code") String code, Limit limit);

  /** 未使用の招待コードとして、指定コードが既に存在するか（生成時の衝突チェック用）。 */
  @Query(
      """
      SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM InvitationCode c
      WHERE c.code = :code
        AND c.usedAt IS NULL
      """)
  boolean existsUnusedByCode(@Param("code") String code);
}
