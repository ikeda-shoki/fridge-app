package com.example.fridgeapp.group;

import com.example.fridgeapp.common.AppError;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

  private static final Duration CODE_VALIDITY = Duration.ofDays(7);
  private static final int MAX_GENERATION_ATTEMPTS = 10;

  private final GroupRepository groupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final InvitationCodeRepository invitationCodeRepository;
  private final InvitationCodeGenerator invitationCodeGenerator;
  private final JoinRateLimiter joinRateLimiter;

  public InvitationService(
      GroupRepository groupRepository,
      GroupMemberRepository groupMemberRepository,
      InvitationCodeRepository invitationCodeRepository,
      InvitationCodeGenerator invitationCodeGenerator,
      JoinRateLimiter joinRateLimiter) {
    this.groupRepository = groupRepository;
    this.groupMemberRepository = groupMemberRepository;
    this.invitationCodeRepository = invitationCodeRepository;
    this.invitationCodeGenerator = invitationCodeGenerator;
    this.joinRateLimiter = joinRateLimiter;
  }

  @Transactional
  public InvitationCodeResponse issueInvitationCode(UUID userId, UUID groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw new GroupException(AppError.GROUP_NOT_FOUND);
    }
    if (!groupMemberRepository.existsMemberWithRole(groupId, userId, GroupRole.OWNER)) {
      throw new GroupException(AppError.NOT_GROUP_OWNER);
    }

    invitationCodeRepository
        .findUnusedByGroupId(groupId)
        .ifPresent(
            existing -> {
              existing.expireNow();
              invitationCodeRepository.save(existing);
            });

    Instant now = Instant.now();
    InvitationCode invitationCode =
        invitationCodeRepository.save(
            new InvitationCode(groupId, generateUniqueCode(), now.plus(CODE_VALIDITY)));
    return InvitationCodeResponse.from(invitationCode);
  }

  @Transactional
  public GroupResponse joinGroup(UUID userId, String rawCode, String clientIp) {
    joinRateLimiter.assertAllowed(clientIp);

    String code = rawCode.trim().toUpperCase();
    Instant now = Instant.now();

    Optional<InvitationCode> maybeCode =
        invitationCodeRepository.findLatestByCode(code, Limit.of(1));
    if (maybeCode.isEmpty()) {
      joinRateLimiter.recordFailure(clientIp);
      throw new GroupException(AppError.INVALID_INVITATION_CODE);
    }

    InvitationCode invitationCode = maybeCode.get();
    if (invitationCode.isLocked(now)) {
      joinRateLimiter.recordFailure(clientIp);
      throw new GroupException(AppError.INVITATION_CODE_LOCKED);
    }
    if (invitationCode.isUsed()) {
      invitationCode.recordFailedAttempt(now);
      invitationCodeRepository.save(invitationCode);
      joinRateLimiter.recordFailure(clientIp);
      throw new GroupException(AppError.INVITATION_CODE_ALREADY_USED);
    }
    if (invitationCode.isExpired(now)) {
      invitationCode.recordFailedAttempt(now);
      invitationCodeRepository.save(invitationCode);
      joinRateLimiter.recordFailure(clientIp);
      throw new GroupException(AppError.INVITATION_CODE_EXPIRED);
    }

    UUID groupId = invitationCode.getGroupId();
    if (groupMemberRepository.existsMember(groupId, userId)) {
      joinRateLimiter.recordSuccess(clientIp);
      throw new GroupException(AppError.ALREADY_GROUP_MEMBER);
    }

    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new GroupException(AppError.GROUP_NOT_FOUND));
    groupMemberRepository.save(new GroupMember(groupId, userId, GroupRole.MEMBER, null));
    invitationCode.markUsed(userId, now);
    invitationCodeRepository.save(invitationCode);
    joinRateLimiter.recordSuccess(clientIp);

    return GroupResponse.from(group);
  }

  private String generateUniqueCode() {
    for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
      String candidate = invitationCodeGenerator.generate();
      if (!invitationCodeRepository.existsUnusedByCode(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("招待コードの生成に失敗しました（衝突が続きました）");
  }
}
