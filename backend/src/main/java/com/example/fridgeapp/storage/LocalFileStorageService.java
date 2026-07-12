package com.example.fridgeapp.storage;

import com.example.fridgeapp.common.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "app.storage",
    name = "type",
    havingValue = "local",
    matchIfMissing = true)
public class LocalFileStorageService implements StorageService {

  private final Path basePath;

  public LocalFileStorageService(AppProperties appProperties) {
    this.basePath = Path.of(appProperties.storage().localPath()).toAbsolutePath().normalize();
    try {
      Files.createDirectories(basePath);
    } catch (IOException e) {
      throw new UncheckedIOException("ストレージディレクトリの作成に失敗しました", e);
    }
  }

  @Override
  public String store(byte[] content, String extension) {
    String fileName = UUID.randomUUID() + "." + extension;
    try {
      Files.write(basePath.resolve(fileName), content);
    } catch (IOException e) {
      throw new UncheckedIOException("ファイルの保存に失敗しました", e);
    }
    return fileName;
  }

  @Override
  public void delete(String path) {
    Path target = basePath.resolve(path).normalize();
    if (!target.startsWith(basePath)) {
      throw new IllegalArgumentException("不正なパスです: " + path);
    }
    try {
      Files.deleteIfExists(target);
    } catch (IOException e) {
      throw new UncheckedIOException("ファイルの削除に失敗しました", e);
    }
  }
}
