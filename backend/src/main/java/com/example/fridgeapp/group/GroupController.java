package com.example.fridgeapp.group;

import com.example.fridgeapp.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** グループ管理 API（GRP-01〜05）。作成・詳細・削除・メンバー一覧・オーナー譲渡・脱退を扱う。 */
@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

  private final GroupService groupService;

  public GroupController(GroupService groupService) {
    this.groupService = groupService;
  }

  /** グループを作成する。作成者はオーナーとして自動的にメンバーになる。 */
  @PostMapping
  public ResponseEntity<GroupResponse> createGroup(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody GroupCreateRequest request) {
    return ResponseEntity.ok(groupService.createGroup(principal.userId(), request.name()));
  }

  /** グループ詳細を返す。メンバーのみ参照できる（非メンバーは 403）。 */
  @GetMapping("/{id}")
  public ResponseEntity<GroupResponse> getGroup(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    return ResponseEntity.ok(groupService.getGroupDetail(principal.userId(), id));
  }

  /** グループを削除する。オーナーのみ実行できる（非オーナーは 403）。 */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteGroup(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    groupService.deleteGroup(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }

  /** メンバー一覧を返す。メンバーのみ参照できる。 */
  @GetMapping("/{id}/members")
  public ResponseEntity<List<GroupMemberResponse>> listMembers(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    return ResponseEntity.ok(groupService.listMembers(principal.userId(), id));
  }

  /** オーナー権限を他のメンバーへ譲渡する。オーナーのみ実行でき、譲渡先はメンバーである必要がある。 */
  @PostMapping("/{id}/members/{userId}/transfer-ownership")
  public ResponseEntity<Void> transferOwnership(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @PathVariable UUID userId) {
    groupService.transferOwnership(principal.userId(), id, userId);
    return ResponseEntity.noContent().build();
  }

  /** 自分がグループを脱退する。唯一のオーナーは脱退できない（先に譲渡または削除が必要）。 */
  @DeleteMapping("/{id}/members/me")
  public ResponseEntity<Void> leaveGroup(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    groupService.leaveGroup(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }
}
