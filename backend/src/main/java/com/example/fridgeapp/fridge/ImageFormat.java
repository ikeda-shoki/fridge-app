package com.example.fridgeapp.fridge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/** マジックバイトで検証済みの受付画像形式（ADR-007: WebP は未対応）。 */
enum ImageFormat {
  JPEG("jpg"),
  PNG("png");

  private final String extension;

  ImageFormat(String extension) {
    this.extension = extension;
  }

  String extension() {
    return extension;
  }

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
