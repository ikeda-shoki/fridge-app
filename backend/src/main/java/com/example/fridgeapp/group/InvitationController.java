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

/** グループ招待 API（GRP-06・GRP-07）。招待コードの発行と、コードによる参加を扱う。 */
@RestController
@RequestMapping("/api/v1/groups")
public class InvitationController {

  private final InvitationService invitationService;

  public InvitationController(InvitationService invitationService) {
    this.invitationService = invitationService;
  }

  /** 招待コードを発行する。オーナーのみ実行でき、そのグループの既存の有効なコードは失効する。 */
  @PostMapping("/{id}/invitations")
  public ResponseEntity<InvitationCodeResponse> issueInvitation(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
    return ResponseEntity.ok(invitationService.issueInvitationCode(principal.userId(), id));
  }

  /**
   * 招待コードでグループに参加する。
   *
   * <p>総当たり対策として、送信元 IP 単位のレートリミットとコード単位のロックを適用するため、クライアント IP を {@link InvitationService} へ渡す。
   */
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
