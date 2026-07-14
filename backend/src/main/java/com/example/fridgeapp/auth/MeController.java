package com.example.fridgeapp.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ログイン中ユーザー情報の API（AUTH-04）。 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

  private final AuthService authService;

  public MeController(AuthService authService) {
    this.authService = authService;
  }

  /** ログイン中のユーザー情報を返す。 */
  @GetMapping
  public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal AuthenticatedUser principal) {
    User user = authService.getUser(principal.userId());
    return ResponseEntity.ok(UserResponse.from(user));
  }
}
