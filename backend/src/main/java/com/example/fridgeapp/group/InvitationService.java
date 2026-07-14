package com.example.fridgeapp.group;

import com.example.fridgeapp.common.AppError;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招待コードの発行と、コードによるグループ参加を扱う。
 *
 * <p>コードは 6 文字と短く総当たりが成立し得るため、多層で防御する。IP 単位のレートリミット・ロック（{@link
 * JoinRateLimiter}）、コード単位の失敗回数ロック（{@link InvitationCode}）、7 日の有効期限、1 回で使い切り。
 */
@Service
public class InvitationService {

  private static final Duration CODE_VALIDITY = Duration.ofDays(7);
  private static final int MAX_GENERATION_ATTEMPTS = 10;

  private final GroupRepository groupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final InvitationCodeRepository invitationCodeRepository;
  private final InvitationCodeGenerator invitationCodeGenerator;
  private final JoinRateLimiter joinRateLimiter;
  private final GroupAccessGuard groupAccessGuard;

  public InvitationService(
      GroupRepository groupRepository,
      GroupMemberRepository groupMemberRepository,
      InvitationCodeRepository invitationCodeRepository,
      InvitationCodeGenerator invitationCodeGenerator,
      JoinRateLimiter joinRateLimiter,
      GroupAccessGuard groupAccessGuard) {
    this.groupRepository = groupRepository;
    this.groupMemberRepository = groupMemberRepository;
    this.invitationCodeRepository = invitationCodeRepository;
    this.invitationCodeGenerator = invitationCodeGenerator;
    this.joinRateLimiter = joinRateLimiter;
    this.groupAccessGuard = groupAccessGuard;
  }

  /**
   * 招待コードを発行する（GRP-06）。有効期限は 7 日。同じグループに未使用のコードが残っている場合は、それを即時失効させてから新しいコードを発行する（有効なコードは常に 1 件）。
   *
   * @throws GroupException グループが存在しない場合（{@link AppError#GROUP_NOT_FOUND}）、操作ユーザーがオーナーでない場合（{@link
   *     AppError#NOT_GROUP_OWNER}）
   */
  @Transactional
  public InvitationCodeResponse issueInvitationCode(UUID userId, UUID groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw new GroupException(AppError.GROUP_NOT_FOUND);
    }
    groupAccessGuard.assertOwner(groupId, userId);

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

  /**
   * 招待コードでグループに参加する（GRP-07）。参加に成功するとコードは使用済みになる。
   *
   * <p>失敗時は総当たり対策として、コード側の失敗回数と IP 側の連続失敗回数の両方を加算する（閾値超過で一定時間ロックされる）。
   *
   * @param clientIp レートリミット・ロックの判定単位となる送信元 IP
   * @throws GroupException レート制限・ロック中（{@link AppError#JOIN_RATE_LIMITED} / {@link
   *     AppError#INVITATION_CODE_LOCKED}）、コードが不正・期限切れ・使用済み、既にメンバーの場合
   */
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
