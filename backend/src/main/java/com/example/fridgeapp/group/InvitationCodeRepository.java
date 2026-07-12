package com.example.fridgeapp.group;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationCodeRepository extends JpaRepository<InvitationCode, UUID> {

  Optional<InvitationCode> findByGroupIdAndUsedAtIsNull(UUID groupId);

  Optional<InvitationCode> findTopByCodeOrderByCreatedAtDesc(String code);

  boolean existsByCodeAndUsedAtIsNull(String code);
}
