package com.example.fridgeapp.group;

import com.example.fridgeapp.auth.User;
import com.example.fridgeapp.auth.UserRepository;
import com.example.fridgeapp.common.AppError;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** グループの作成・参照・削除、メンバー一覧・脱退・オーナー譲渡を扱う。 */
@Service
public class GroupService {

  private final GroupRepository groupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final UserRepository userRepository;
  private final GroupAccessGuard groupAccessGuard;

  public GroupService(
      GroupRepository groupRepository,
      GroupMemberRepository groupMemberRepository,
      UserRepository userRepository,
      GroupAccessGuard groupAccessGuard) {
    this.groupRepository = groupRepository;
    this.groupMemberRepository = groupMemberRepository;
    this.userRepository = userRepository;
    this.groupAccessGuard = groupAccessGuard;
  }

  /** グループを作成する（GRP-01）。作成者をオーナーとして所属させる。 */
  @Transactional
  public GroupResponse createGroup(UUID userId, String name) {
    Group group = groupRepository.save(new Group(name));
    groupMemberRepository.save(new GroupMember(group.getId(), userId, GroupRole.OWNER, null));
    return GroupResponse.from(group);
  }

  /**
   * グループ詳細を取得する（GRP-02）。
   *
   * @throws GroupException グループが存在しない場合（{@link AppError#GROUP_NOT_FOUND}）、操作ユーザーがメンバーでない場合（{@link
   *     AppError#NOT_GROUP_MEMBER}）
   */
  @Transactional(readOnly = true)
  public GroupResponse getGroupDetail(UUID userId, UUID groupId) {
    Group group = findGroup(groupId);
    groupAccessGuard.assertMember(groupId, userId);
    return GroupResponse.from(group);
  }

  /**
   * グループを削除する（GRP-03）。関連するメンバー・アイテム等も DB の外部キー制約により連鎖削除される。
   *
   * @throws GroupException グループが存在しない場合（{@link AppError#GROUP_NOT_FOUND}）、操作ユーザーがオーナーでない場合（{@link
   *     AppError#NOT_GROUP_OWNER}）
   */
  @Transactional
  public void deleteGroup(UUID userId, UUID groupId) {
    Group group = findGroup(groupId);
    groupAccessGuard.assertOwner(groupId, userId);
    groupRepository.delete(group);
  }

  /**
   * メンバー一覧を取得する（GRP-04）。
   *
   * @throws GroupException グループが存在しない場合（{@link AppError#GROUP_NOT_FOUND}）、操作ユーザーがメンバーでない場合（{@link
   *     AppError#NOT_GROUP_MEMBER}）
   */
  @Transactional(readOnly = true)
  public List<GroupMemberResponse> listMembers(UUID userId, UUID groupId) {
    findGroup(groupId);
    groupAccessGuard.assertMember(groupId, userId);
    return groupMemberRepository.findByGroupId(groupId).stream()
        .map(
            member -> {
              User user =
                  userRepository
                      .findById(member.getUserId())
                      .orElseThrow(() -> new GroupException(AppError.USER_NOT_FOUND));
              return GroupMemberResponse.of(member, user);
            })
        .toList();
  }

  /**
   * グループから脱退する（GRP-05）。グループを無主にしないため、唯一のオーナーは脱退できない。
   *
   * @throws GroupException メンバーでない場合（{@link AppError#NOT_GROUP_MEMBER}）、唯一のオーナーが脱退しようとした場合（{@link
   *     AppError#LAST_OWNER_CANNOT_LEAVE}）
   */
  @Transactional
  public void leaveGroup(UUID userId, UUID groupId) {
    findGroup(groupId);
    GroupMember member = findMember(groupId, userId);
    if (member.getRole() == GroupRole.OWNER
        && groupMemberRepository.countMembersWithRole(groupId, GroupRole.OWNER) <= 1) {
      throw new GroupException(AppError.LAST_OWNER_CANNOT_LEAVE);
    }
    groupMemberRepository.delete(member);
  }

  /**
   * オーナー権限を他のメンバーへ譲渡する。譲渡元は MEMBER に降格せず、オーナーが複数になる点に注意。
   *
   * @throws GroupException 操作ユーザーがオーナーでない場合（{@link AppError#NOT_GROUP_OWNER}）、譲渡先がメンバーでない場合（{@link
   *     AppError#TARGET_USER_NOT_GROUP_MEMBER}）
   */
  @Transactional
  public void transferOwnership(UUID requesterId, UUID groupId, UUID targetUserId) {
    findGroup(groupId);
    groupAccessGuard.assertOwner(groupId, requesterId);
    GroupMember target =
        groupMemberRepository
            .findMember(groupId, targetUserId)
            .orElseThrow(() -> new GroupException(AppError.TARGET_USER_NOT_GROUP_MEMBER));
    target.promoteToOwner();
    groupMemberRepository.save(target);
  }

  private Group findGroup(UUID groupId) {
    return groupRepository
        .findById(groupId)
        .orElseThrow(() -> new GroupException(AppError.GROUP_NOT_FOUND));
  }

  private GroupMember findMember(UUID groupId, UUID userId) {
    return groupMemberRepository
        .findMember(groupId, userId)
        .orElseThrow(() -> new GroupException(AppError.NOT_GROUP_MEMBER));
  }
}
