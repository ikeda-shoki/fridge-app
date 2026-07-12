package com.example.fridgeapp.group;

import com.example.fridgeapp.auth.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
public class InvitationController {

  private final InvitationService invitationService;

  public InvitationController(InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  @PostMapping("/{id}/invitations")
  public ResponseEntity<InvitationCodeResponse> issueInvitation(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    return ResponseEntity.ok(invitationService.issueInvitationCode(principal.userId(), id));
  }

  @PostMapping("/join")
  public ResponseEntity<GroupResponse> joinGroup(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody JoinGroupRequest request,
      HttpServletRequest servletRequest) {
    GroupResponse response =
        invitationService.joinGroup(
            principal.userId(), request.code(), servletRequest.getRemoteAddr());
    return ResponseEntity.ok(response);
  }
}
