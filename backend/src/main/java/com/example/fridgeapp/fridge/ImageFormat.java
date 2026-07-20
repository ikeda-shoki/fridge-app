package com.example.fridgeapp.fridge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/** マジックバイトで検証済みの受付画像形式（ADR-007: WebP は未対応）。 */
enum ImageFormat {
  JPEG("jpg", "image/jpeg"),
  PNG("png", "image/png");

  private final String extension;
  private final String contentType;

  ImageFormat(String extension, String contentType) {
    this.extension = extension;
    this.contentType = contentType;
  }

  /** 保存時に使う拡張子。 */
  String extension() {
    return extension;
  }

  /** 配信時に返す Content-Type。 */
  String contentType() {
    return contentType;
  }

  /**
   * 保存済みパス（{@code {UUID}.jpg} 形式）の拡張子から画像形式を引く。判定できない場合は空を返す。
   *
   * <p>保存時にマジックバイトで判定した形式の拡張子しか付けないため、ここでは拡張子を信頼してよい。
   */
  static Optional<ImageFormat> fromStoredPath(String path) {
    int dotIndex = path.lastIndexOf('.');
    if (dotIndex < 0) {
      return Optional.empty();
    }
    String extension = path.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(format -> format.extension.equals(extension)).findFirst();
  }

  /**
   * 先頭バイト（マジックバイト）から画像形式を判定する。判定できない場合は空を返す。
   *
   * <p>Content-Type やファイル名の拡張子はクライアントが自由に詐称できるため、実体のバイト列で判定する。
   */
  static Optional<ImageFormat> detect(MultipartFile file) {
    byte[] header = new byte[8];
    int read;
    try (InputStream in = file.getInputStream()) {
      read = in.readNBytes(header, 0, header.length);
    } catch (IOException e) {
      return Optional.empty();
    }
    if (read >= 3
        && (header[0] & 0xFF) == 0xFF
        && (header[1] & 0xFF) == 0xD8
        && (header[2] & 0xFF) == 0xFF) {
      return Optional.of(JPEG);
    }
    if (read >= 4
        && (header[0] & 0xFF) == 0x89
        && header[1] == 'P'
        && header[2] == 'N'
        && header[3] == 'G') {
      return Optional.of(PNG);
    }
    return Optional.empty();
  }
}
