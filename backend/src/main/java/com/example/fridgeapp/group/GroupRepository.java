package com.example.fridgeapp.group;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** グループの永続化。 */
public interface GroupRepository extends JpaRepository<Group, UUID> {

  /** ユーザーが所属する全グループを、参加日時の古い順で返す（GRP-00 の判定・GET /me で使用）。 */
  @Query(
      """
      SELECT g FROM Group g
      JOIN GroupMember m ON m.groupId = g.id
      WHERE m.userId = :userId
      ORDER BY m.joinedAt ASC
      """)
  List<Group> findAllByMemberUserId(@Param("userId") UUID userId);
}
