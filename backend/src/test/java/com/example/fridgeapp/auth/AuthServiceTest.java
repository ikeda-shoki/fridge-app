package com.example.fridgeapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.group.GroupMemberRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private GoogleIdTokenVerifierService googleVerifier;
  @Mock private GroupMemberRepository groupMemberRepository;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            userRepository, jwtService, refreshTokenService, googleVerifier, groupMemberRepository);
  }

  @Test
  void deleteAccountMarksUserDeletedAndRevokesTokens() {
    UUID userId = UUID.randomUUID();
    User user = activeUser(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(groupMemberRepository.hasSoleOwnershipOfAnyGroup(userId)).thenReturn(false);

    authService.deleteAccount(userId);

    assertThat(user.isDeleted()).isTrue();
    verify(userRepository).save(user);
    verify(refreshTokenService).revokeAllForUser(userId);
  }

  @Test
  void deleteAccountThrowsWhenUserIsSoleOwnerOfAnyGroup() {
    UUID userId = UUID.randomUUID();
    User user = activeUser(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(groupMemberRepository.hasSoleOwnershipOfAnyGroup(userId)).thenReturn(true);

    assertThatThrownBy(() -> authService.deleteAccount(userId))
        .isInstanceOf(AuthException.class)
        .extracting("error")
        .isEqualTo(AppError.ACCOUNT_HAS_SOLE_OWNERSHIP);

    assertThat(user.isDeleted()).isFalse();
    verify(userRepository, never()).save(user);
    verify(refreshTokenService, never()).revokeAllForUser(userId);
  }

  @Test
  void deleteAccountThrowsWhenUserNotFound() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.deleteAccount(userId))
        .isInstanceOf(AuthException.class)
        .extracting("error")
        .isEqualTo(AppError.USER_NOT_FOUND);
  }

  @Test
  void deleteAccountThrowsWhenUserAlreadyDeleted() {
    UUID userId = UUID.randomUUID();
    User user = activeUser(userId);
    user.markDeleted();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.deleteAccount(userId))
        .isInstanceOf(AuthException.class)
        .extracting("error")
        .isEqualTo(AppError.USER_NOT_FOUND);
  }

  private User activeUser(UUID userId) {
    User user = new User("google-sub", "テスト太郎", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
