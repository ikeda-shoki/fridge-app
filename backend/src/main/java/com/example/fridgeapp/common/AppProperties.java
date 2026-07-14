package com.example.fridgeapp.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code app.*} で定義するアプリ固有の設定値。
 *
 * <p>値は環境変数（`.env`）から注入される。必須項目が欠けている・形式が不正な場合は起動時に検証エラーとする（実行時まで問題を持ち越さないため）。
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
    @Valid Jwt jwt,
    @Valid Google google,
    @Valid Cookie cookie,
    @Valid Cors cors,
    @Valid Storage storage) {

  public record Jwt(
      /**
       * HMAC-SHA の署名鍵。UTF-8 バイト列をそのまま鍵として使うため、RFC 7518 が要求する 256bit を満たすには 32 文字以上が必要 （不足すると起動時に
       * jjwt が WeakKeyException を投げる）。
       */
      @NotBlank @Size(min = 32, message = "JWT_SECRET は 32 文字以上にしてください（HMAC-SHA の鍵長 256bit 以上が必要）")
          String secret,
      @NotBlank String issuer,
      @NotBlank String audience,
      @NotNull Duration accessTokenExpiry,
      @NotNull Duration refreshTokenExpiry) {}

  public record Google(@NotBlank String clientId) {}

  public record Cookie(boolean secure) {}

  public record Cors(@NotEmpty List<String> allowedOrigins) {}

  public record Storage(@NotBlank String type, @NotBlank String localPath) {}
}
