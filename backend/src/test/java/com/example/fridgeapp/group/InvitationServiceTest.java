package com.example.fridgeapp.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fridgeapp.common.AppError;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private InvitationCodeRepository invitationCodeRepository;
  @Mock private InvitationCodeGenerator invitationCodeGenerator;
  @Mock private JoinRateLimiter joinRateLimiter;

  private InvitationService invitationService;

  @BeforeEach
  void setUp() {
    invitationService =
        new InvitationService(
            groupRepository,
            groupMemberRepository,
            invitationCodeRepository,
            invitationCodeGenerator,
            joinRateLimiter);
  }

  @Test
  void issueInvitationCodeThrowsWhenNotOwner() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(groupRepository.existsById(groupId)).thenReturn(true);
    when(groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, GroupRole.OWNER))
        .thenReturn(false);

    assertThatThrownBy(() -> invitationService.issueInvitationCode(userId, groupId))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.NOT_GROUP_OWNER);
  }

  @Test
  void issueInvitationCodeExpiresExistingActiveCodeAndCreatesNewOne() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(groupRepository.existsById(groupId)).thenReturn(true);
    when(groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, GroupRole.OWNER))
        .thenReturn(true);
    InvitationCode existing = new InvitationCode(groupId, "OLDCOD", Instant.now().plusSeconds(600));
    when(invitationCodeRepository.findByGroupIdAndUsedAtIsNull(groupId))
        .thenReturn(Optional.of(existing));
    when(invitationCodeGenerator.generate()).thenReturn("NEWCOD");
    when(invitationCodeRepository.existsByCodeAndUsedAtIsNull("NEWCOD")).thenReturn(false);
    when(invitationCodeRepository.save(any(InvitationCode.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InvitationCodeResponse response = invitationService.issueInvitationCode(userId, groupId);

    assertThat(response.code()).isEqualTo("NEWCOD");
    assertThat(existing.getExpiresAt()).isBeforeOrEqualTo(Instant.now());
    verify(invitationCodeRepository, times(2)).save(any(InvitationCode.class));
  }

  @Test
  void joinGroupThrowsWhenCodeNotFound() {
    when(invitationCodeRepository.findTopByCodeOrderByCreatedAtDesc("ABCDEF"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> invitationService.joinGroup(UUID.randomUUID(), "abcdef", "192.0.2.1"))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.INVALID_INVITATION_CODE);

    verify(joinRateLimiter).recordFailure("192.0.2.1");
  }

  @Test
  void joinGroupThrowsWhenCodeLocked() {
    UUID groupId = UUID.randomUUID();
    InvitationCode code = new InvitationCode(groupId, "ABCDEF", Instant.now().plusSeconds(600));
    for (int i = 0; i < 10; i++) {
      code.recordFailedAttempt(Instant.now());
    }
    when(invitationCodeRepository.findTopByCodeOrderByCreatedAtDesc("ABCDEF"))
        .thenReturn(Optional.of(code));

    assertThatThrownBy(() -> invitationService.joinGroup(UUID.randomUUID(), "ABCDEF", "192.0.2.1"))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.INVITATION_CODE_LOCKED);
  }

  @Test
  void joinGroupThrowsWhenCodeExpired() {
    UUID groupId = UUID.randomUUID();
    InvitationCode code =
        new InvitationCode(groupId, "ABCDEF", Instant.now().minus(1, ChronoUnit.DAYS));
    when(invitationCodeRepository.findTopByCodeOrderByCreatedAtDesc("ABCDEF"))
        .thenReturn(Optional.of(code));

    assertThatThrownBy(() -> invitationService.joinGroup(UUID.randomUUID(), "ABCDEF", "192.0.2.1"))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.INVITATION_CODE_EXPIRED);

    assertThat(code.getFailedAttempts()).isEqualTo(1);
  }

  @Test
  void joinGroupThrowsWhenCodeAlreadyUsed() {
    UUID groupId = UUID.randomUUID();
    InvitationCode code = new InvitationCode(groupId, "ABCDEF", Instant.now().plusSeconds(600));
    code.markUsed(UUID.randomUUID(), Instant.now().minusSeconds(60));
    when(invitationCodeRepository.findTopByCodeOrderByCreatedAtDesc("ABCDEF"))
        .thenReturn(Optional.of(code));

    assertThatThrownBy(() -> invitationService.joinGroup(UUID.randomUUID(), "ABCDEF", "192.0.2.1"))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.INVITATION_CODE_ALREADY_USED);
  }

  @Test
  void joinGroupThrowsWhenAlreadyMember() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    InvitationCode code = new InvitationCode(groupId, "ABCDEF", Instant.now().plusSeconds(600));
    when(invitationCodeRepository.findTopByCodeOrderByCreatedAtDesc("ABCDEF"))
        .thenReturn(Optional.of(code));
    when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(true);

    assertThatThrownBy(() -> invitationService.joinGroup(userId, "ABCDEF", "192.0.2.1"))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.ALREADY_GROUP_MEMBER);

    verify(joinRateLimiter, never()).recordFailure(anyString());
  }

  @Test
  void joinGroupSucceedsAndMarksCodeUsed() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    InvitationCode code = new InvitationCode(groupId, "ABCDEF", Instant.now().plusSeconds(600));
    when(invitationCodeRepository.findTopByCodeOrderByCreatedAtDesc("ABCDEF"))
        .thenReturn(Optional.of(code));
    when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(false);
    Group group = new Group("テストグループ");
    ReflectionTestUtils.setField(group, "id", groupId);
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

    GroupResponse response = invitationService.joinGroup(userId, "abcdef", "192.0.2.1");

    assertThat(response.id()).isEqualTo(groupId);
    assertThat(code.isUsed()).isTrue();
    verify(groupMemberRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                member ->
                    member.getUserId().equals(userId) && member.getRole() == GroupRole.MEMBER));
    verify(joinRateLimiter).recordSuccess("192.0.2.1");
  }
}
