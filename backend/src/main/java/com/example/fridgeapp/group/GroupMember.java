package com.example.fridgeapp.group;

import com.example.fridgeapp.common.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** グループへの所属。ユーザーとグループの対応と、そのグループでの役割（{@link GroupRole}）を持つ。 */
@Entity
@Table(name = "group_members")
public class GroupMember extends AbstractAuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "group_id", nullable = false, updatable = false)
  private UUID groupId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private GroupRole role;

  @Column(name = "invited_by", updatable = false)
  private UUID invitedBy;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private Instant joinedAt;

  protected GroupMember() {}

  public GroupMember(UUID groupId, UUID userId, GroupRole role, UUID invitedBy) {
    this.groupId = groupId;
    this.userId = userId;
    this.role = role;
    this.invitedBy = invitedBy;
    this.joinedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public UUID getUserId() {
    return userId;
  }

  public GroupRole getRole() {
    return role;
  }

  public UUID getInvitedBy() {
    return invitedBy;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  /** オーナーへ昇格させる（オーナー譲渡時）。 */
  public void promoteToOwner() {
    this.role = GroupRole.OWNER;
  }
}
