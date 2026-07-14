package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.common.AppProperties;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * リフレッシュトークンの発行・検証・失効を行う。
 *
 * <p>DB には生トークンを保存せず SHA-256 ハッシュのみを保持する（DB 流出時にトークンをそのまま使われないようにするため）。呼び出し側へ返すのは生トークンで、これを Cookie
 * に格納する。
 */
@Service
public class RefreshTokenService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final RefreshTokenRepository refreshTokenRepository;
  private final AppProperties.Jwt jwtProps;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AppProperties props) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtProps = props.jwt();
  }

  /** 256bit ランダムトークンを生成し SHA-256 ハッシュを DB に保存。生トークンを返す。 */
  @Transactional
  public String issue(User user) {
    String rawToken = generateRawToken();
    String tokenHash = sha256Hex(rawToken);
    Instant expiresAt = Instant.now().plus(jwtProps.refreshTokenExpiry());
    refreshTokenRepository.save(new RefreshToken(user, tokenHash, expiresAt));
    return rawToken;
  }

  /**
   * 生トークンから有効なリフレッシュトークンを取得する。
   *
   * @throws AuthException 該当トークンが無い、期限切れ、または失効済みの場合（{@link AppError#INVALID_REFRESH_TOKEN}）
   */
  @Transactional(readOnly = true)
  public RefreshToken findValid(String rawToken) {
    String tokenHash = sha256Hex(rawToken);
    RefreshToken token =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthException(AppError.INVALID_REFRESH_TOKEN));
    if (!token.isValid()) {
      throw new AuthException(AppError.INVALID_REFRESH_TOKEN);
    }
    return token;
  }

  /** 旧トークンを失効させ新トークンを発行（ローテーション）。 */
  @Transactional
  public String rotate(RefreshToken oldToken) {
    oldToken.revoke();
    refreshTokenRepository.save(oldToken);
    return issue(oldToken.getUser());
  }

  /** トークンを失効させる（ログアウト時）。該当トークンが無い場合は何もしない。 */
  @Transactional
  public void revoke(String rawToken) {
    String tokenHash = sha256Hex(rawToken);
    refreshTokenRepository
        .findByTokenHash(tokenHash)
        .ifPresent(
            token -> {
              token.revoke();
              refreshTokenRepository.save(token);
            });
  }

  /** 対象ユーザーの有効なリフレッシュトークンをすべて失効させる（退会時に全端末のセッションを切るため）。 */
  @Transactional
  public void revokeAllForUser(UUID userId) {
    refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
  }

  private String generateRawToken() {
    byte[] bytes = new byte[32]; // 256 bits
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String sha256Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(input.getBytes());
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
