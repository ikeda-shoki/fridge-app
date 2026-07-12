package com.example.fridgeapp.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

  boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

  boolean existsByGroupIdAndUserIdAndRole(UUID groupId, UUID userId, GroupRole role);

  Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

  List<GroupMember> findByGroupId(UUID groupId);

  long countByGroupIdAndRole(UUID groupId, GroupRole role);
}
