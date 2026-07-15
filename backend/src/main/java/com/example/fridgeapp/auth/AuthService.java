package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.group.GroupMemberRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 認証の中核処理。Google ID トークンの検証、ユーザーの登録・更新、トークンの発行・更新・失効を扱う。 */
@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final GoogleIdTokenVerifierService googleVerifier;
  private final GroupMemberRepository groupMemberRepository;

  public AuthService(
      UserRepository userRepository,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      GoogleIdTokenVerifierService googleVerifier,
      GroupMemberRepository groupMemberRepository) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.googleVerifier = googleVerifier;
    this.groupMemberRepository = groupMemberRepository;
  }

  /**
   * Google ID トークンを検証してログインする（AUTH-01）。初回ログインならユーザーを新規登録し、既存ユーザーなら表示名・アバターを最新のプロフィールへ更新する。
   *
   * @throws AuthException トークンが不正な場合（{@link AppError#INVALID_GOOGLE_TOKEN}）、退会済みアカウントの場合（{@link
   *     AppError#ACCOUNT_DELETED}）
   */
  @Transactional
  public LoginResult loginWithGoogle(String idTokenString) {
    Payload payload = googleVerifier.verify(idTokenString);

    String googleSub = payload.getSubject();
    String displayName = (String) payload.get("name");
    String avatarUrl = (String) payload.get("picture");

    User user =
        userRepository
            .findByGoogleSub(googleSub)
            .map(
                existing -> {
                  if (existing.isDeleted()) {
                    throw new AuthException(AppError.ACCOUNT_DELETED);
                  }
                  existing.updateProfile(displayName, avatarUrl);
                  return existing;
                })
            .orElseGet(() -> userRepository.save(new User(googleSub, displayName, avatarUrl)));

    log.info("User logged in: id={}", user.getId());
    return new LoginResult(issueTokens(user), user);
  }

  /**
   * リフレッシュトークンからアクセストークンを再発行する（AUTH-02）。使用したリフレッシュトークンは失効し、新しいものへローテーションされる。
   *
   * @throws AuthException トークンが無効・期限切れ・失効済みの場合（{@link
   *     AppError#INVALID_REFRESH_TOKEN}）、退会済みアカウントの場合（{@link AppError#ACCOUNT_DELETED}）
   */
  @Transactional
  public AuthTokens refresh(String rawRefreshToken) {
    RefreshToken oldToken = refreshTokenService.findValid(rawRefreshToken);
    User user = oldToken.getUser();

    if (user.isDeleted()) {
      throw new AuthException(AppError.ACCOUNT_DELETED);
    }

    String newAccessToken = jwtService.issueAccessToken(user.getId());
    String newRefreshToken = refreshTokenService.rotate(oldToken);
    return new AuthTokens(newAccessToken, newRefreshToken);
  }

  /** ログアウトする（AUTH-03）。リフレッシュトークンを失効させる。未知のトークンでも例外にはしない。 */
  @Transactional
  public void logout(String rawRefreshToken) {
    refreshTokenService.revoke(rawRefreshToken);
  }

  /**
   * ログイン中のユーザーを取得する。退会済みユーザーは存在しないものとして扱う。
   *
   * @throws AuthException ユーザーが存在しない、または退会済みの場合（{@link AppError#USER_NOT_FOUND}）
   */
  @Transactional(readOnly = true)
  public User getUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(u -> !u.isDeleted())
        .orElseThrow(() -> new AuthException(AppError.USER_NOT_FOUND));
  }

  /**
   * 退会する（AUTH-05）。論理削除し、全端末のリフレッシュトークンを失効させる。
   *
   * <p>唯一のオーナーとなっているグループがある場合は、オーナー譲渡またはグループ削除を先に行う必要がある。
   *
   * @throws AuthException ユーザーが存在しない・既に退会済みの場合（{@link
   *     AppError#USER_NOT_FOUND}）、唯一のオーナーとなっているグループがある場合（{@link
   *     AppError#ACCOUNT_HAS_SOLE_OWNERSHIP}）
   */
  @Transactional
  public void deleteAccount(UUID userId) {
    User user = getUser(userId);
    if (groupMemberRepository.hasSoleOwnershipOfAnyGroup(userId)) {
      throw new AuthException(AppError.ACCOUNT_HAS_SOLE_OWNERSHIP);
    }

    user.markDeleted();
    userRepository.save(user);
    refreshTokenService.revokeAllForUser(userId);
    log.info("User deleted account: id={}", userId);
  }

  private AuthTokens issueTokens(User user) {
    String accessToken = jwtService.issueAccessToken(user.getId());
    String refreshToken = refreshTokenService.issue(user);
    return new AuthTokens(accessToken, refreshToken);
  }
}
