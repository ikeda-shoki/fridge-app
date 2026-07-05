package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final GoogleIdTokenVerifierService googleVerifier;

  public AuthService(
      UserRepository userRepository,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      GoogleIdTokenVerifierService googleVerifier) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.googleVerifier = googleVerifier;
  }

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

  @Transactional
  public void logout(String rawRefreshToken) {
    refreshTokenService.revoke(rawRefreshToken);
  }

  @Transactional(readOnly = true)
  public User getUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(u -> !u.isDeleted())
        .orElseThrow(() -> new AuthException(AppError.USER_NOT_FOUND));
  }

  private AuthTokens issueTokens(User user) {
    String accessToken = jwtService.issueAccessToken(user.getId());
    String refreshToken = refreshTokenService.issue(user);
    return new AuthTokens(accessToken, refreshToken);
  }
}
