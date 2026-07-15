package com.example.fridgeapp.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** グループ所属の永続化。認可判定（メンバーか・オーナーか）にも使う。 */
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

  /** グループに所属する全メンバー。 */
  List<GroupMember> findByGroupId(UUID groupId);

  /** 指定ユーザーがグループに所属しているか。 */
  @Query(
      """
      SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM GroupMember m
      WHERE m.groupId = :groupId
        AND m.userId = :userId
      """)
  boolean existsMember(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

  /** 指定ユーザーが、グループ内で指定ロールを持っているか。 */
  @Query(
      """
      SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM GroupMember m
      WHERE m.groupId = :groupId
        AND m.userId = :userId
        AND m.role = :role
      """)
  boolean existsMemberWithRole(
      @Param("groupId") UUID groupId, @Param("userId") UUID userId, @Param("role") GroupRole role);

  /** グループ内の指定ユーザーのメンバーシップを取得する。 */
  @Query(
      """
      SELECT m FROM GroupMember m
      WHERE m.groupId = :groupId
        AND m.userId = :userId
      """)
  Optional<GroupMember> findMember(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

  /** グループ内で指定ロールを持つメンバー数。 */
  @Query(
      """
      SELECT COUNT(m) FROM GroupMember m
      WHERE m.groupId = :groupId
        AND m.role = :role
      """)
  long countMembersWithRole(@Param("groupId") UUID groupId, @Param("role") GroupRole role);

  /** ユーザーが唯一のオーナーであるグループを 1 件でも持つか（退会時のブロック判定に使用。AUTH-05）。 */
  @Query(
      """
      SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM GroupMember m
      WHERE m.userId = :userId
        AND m.role = com.example.fridgeapp.group.GroupRole.OWNER
        AND (SELECT COUNT(m2) FROM GroupMember m2
             WHERE m2.groupId = m.groupId
               AND m2.role = com.example.fridgeapp.group.GroupRole.OWNER) = 1
      """)
  boolean hasSoleOwnershipOfAnyGroup(@Param("userId") UUID userId);
}
