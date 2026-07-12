package com.example.fridgeapp.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fridgeapp.auth.User;
import com.example.fridgeapp.auth.UserRepository;
import com.example.fridgeapp.common.AppError;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private UserRepository userRepository;

  private GroupService groupService;

  @BeforeEach
  void setUp() {
    groupService = new GroupService(groupRepository, groupMemberRepository, userRepository);
  }

  @Test
  void createGroupSavesGroupAndOwnerMembership() {
    UUID userId = UUID.randomUUID();
    when(groupRepository.save(any(Group.class)))
        .thenAnswer(
            invocation -> {
              Group group = invocation.getArgument(0);
              ReflectionTestUtils.setField(group, "id", UUID.randomUUID());
              return group;
            });

    GroupResponse response = groupService.createGroup(userId, "テスト家族");

    assertThat(response.name()).isEqualTo("テスト家族");
    verify(groupMemberRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                member ->
                    member.getUserId().equals(userId) && member.getRole() == GroupRole.OWNER));
  }

  @Test
  void getGroupDetailThrowsWhenNotMember() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupWithId(groupId)));
    when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(false);

    assertThatThrownBy(() -> groupService.getGroupDetail(userId, groupId))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.NOT_GROUP_MEMBER);
  }

  @Test
  void getGroupDetailThrowsWhenGroupNotFound() {
    UUID groupId = UUID.randomUUID();
    when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> groupService.getGroupDetail(UUID.randomUUID(), groupId))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.GROUP_NOT_FOUND);
  }

  @Test
  void deleteGroupThrowsWhenNotOwner() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupWithId(groupId)));
    when(groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, GroupRole.OWNER))
        .thenReturn(false);

    assertThatThrownBy(() -> groupService.deleteGroup(userId, groupId))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.NOT_GROUP_OWNER);
  }

  @Test
  void deleteGroupSucceedsForOwner() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Group group = groupWithId(groupId);
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(groupMemberRepository.existsByGroupIdAndUserIdAndRole(groupId, userId, GroupRole.OWNER))
        .thenReturn(true);

    groupService.deleteGroup(userId, groupId);

    verify(groupRepository).delete(group);
  }

  @Test
  void listMembersReturnsResolvedUserDetails() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID memberUserId = UUID.randomUUID();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupWithId(groupId)));
    when(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(true);
    GroupMember member = new GroupMember(groupId, memberUserId, GroupRole.MEMBER, userId);
    when(groupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(member));
    User user = new User("google-sub", "テスト太郎", null);
    ReflectionTestUtils.setField(user, "id", memberUserId);
    when(userRepository.findById(memberUserId)).thenReturn(Optional.of(user));

    List<GroupMemberResponse> members = groupService.listMembers(userId, groupId);

    assertThat(members).hasSize(1);
    assertThat(members.get(0).displayName()).isEqualTo("テスト太郎");
    assertThat(members.get(0).role()).isEqualTo(GroupRole.MEMBER);
  }

  @Test
  void leaveGroupThrowsWhenSoleOwnerTriesToLeave() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupWithId(groupId)));
    GroupMember member = new GroupMember(groupId, userId, GroupRole.OWNER, null);
    when(groupMemberRepository.findByGroupIdAndUserId(groupId, userId))
        .thenReturn(Optional.of(member));
    when(groupMemberRepository.countByGroupIdAndRole(groupId, GroupRole.OWNER)).thenReturn(1L);

    assertThatThrownBy(() -> groupService.leaveGroup(userId, groupId))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.LAST_OWNER_CANNOT_LEAVE);
  }

  @Test
  void leaveGroupSucceedsWhenAnotherOwnerExists() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupWithId(groupId)));
    GroupMember member = new GroupMember(groupId, userId, GroupRole.OWNER, null);
    when(groupMemberRepository.findByGroupIdAndUserId(groupId, userId))
        .thenReturn(Optional.of(member));
    when(groupMemberRepository.countByGroupIdAndRole(groupId, GroupRole.OWNER)).thenReturn(2L);

    groupService.leaveGroup(userId, groupId);

    verify(groupMemberRepository).delete(member);
  }

  @Test
  void transferOwnershipThrowsWhenTargetNotMember() {
    UUID groupId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupWithId(groupId)));
    when(groupMemberRepository.existsByGroupIdAndUserIdAndRole(
            groupId, requesterId, GroupRole.OWNER))
        .thenReturn(true);
    when(groupMemberRepository.findByGroupIdAndUserId(groupId, targetId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> groupService.transferOwnership(requesterId, groupId, targetId))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.TARGET_USER_NOT_GROUP_MEMBER);
  }

  @Test
  void transferOwnershipPromotesTargetToOwner() {
    UUID groupId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(groupWithId(groupId)));
    when(groupMemberRepository.existsByGroupIdAndUserIdAndRole(
            groupId, requesterId, GroupRole.OWNER))
        .thenReturn(true);
    GroupMember target = new GroupMember(groupId, targetId, GroupRole.MEMBER, requesterId);
    when(groupMemberRepository.findByGroupIdAndUserId(groupId, targetId))
        .thenReturn(Optional.of(target));

    groupService.transferOwnership(requesterId, groupId, targetId);

    assertThat(target.getRole()).isEqualTo(GroupRole.OWNER);
    verify(groupMemberRepository).save(target);
  }

  private Group groupWithId(UUID groupId) {
    Group group = new Group("テストグループ");
    ReflectionTestUtils.setField(group, "id", groupId);
    return group;
  }
}
