package com.example.fridgeapp.group;

import com.example.fridgeapp.common.AppError;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * グループに対する操作権限を検証する。冷蔵庫アイテム・買い物リストなど、グループに属するリソースを扱う Service から利用する。
 *
 * <p>{@code group} パッケージに置くのは、判定が {@link GroupMemberRepository} とグループの権限ルールに依存するため。
 */
@Component
public class GroupAccessGuard {

  private final GroupMemberRepository groupMemberRepository;

  public GroupAccessGuard(GroupMemberRepository groupMemberRepository) {
    this.groupMemberRepository = groupMemberRepository;
  }

  /** 操作ユーザーがグループのメンバーであることを検証する。 */
  public void assertMember(UUID groupId, UUID userId) {
    if (!groupMemberRepository.existsMember(groupId, userId)) {
      throw new GroupException(AppError.NOT_GROUP_MEMBER);
    }
  }

  /** 操作ユーザーがグループのオーナーであることを検証する。 */
  public void assertOwner(UUID groupId, UUID userId) {
    if (!groupMemberRepository.existsMemberWithRole(groupId, userId, GroupRole.OWNER)) {
      throw new GroupException(AppError.NOT_GROUP_OWNER);
    }
  }
}
