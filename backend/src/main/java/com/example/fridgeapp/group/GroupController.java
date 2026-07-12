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

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

  private final GroupService groupService;

  public GroupController(GroupService groupService) {
    this.groupService = groupService;
  }

  @PostMapping
  public ResponseEntity<GroupResponse> createGroup(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody GroupCreateRequest request) {
    return ResponseEntity.ok(groupService.createGroup(principal.userId(), request.name()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<GroupResponse> getGroup(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    return ResponseEntity.ok(groupService.getGroupDetail(principal.userId(), id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteGroup(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    groupService.deleteGroup(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/members")
  public ResponseEntity<List<GroupMemberResponse>> listMembers(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    return ResponseEntity.ok(groupService.listMembers(principal.userId(), id));
  }

  @PostMapping("/{id}/members/{userId}/transfer-ownership")
  public ResponseEntity<Void> transferOwnership(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable UUID id,
      @PathVariable UUID userId) {
    groupService.transferOwnership(principal.userId(), id, userId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/members/me")
  public ResponseEntity<Void> leaveGroup(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    groupService.leaveGroup(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }
}
