package com.example.fridgeapp.storage;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.common.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * ローカルファイルシステムへ保存する {@link StorageService} 実装（開発用）。{@code app.storage.type=local} のときに有効。
 *
 * <p>ファイル名は UUID で採番し、元のファイル名は使わない。削除時はパスを正規化してベースディレクトリ配下かを検証し、{@code ../} によるディレクトリトラバーサルを防ぐ。
 */
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
      throw new StorageException(AppError.STORAGE_DIRECTORY_CREATE_FAILED, e);
    }
  }

  @Override
  public String store(byte[] content, String extension) {
    String fileName = UUID.randomUUID() + "." + extension;
    try {
      Files.write(basePath.resolve(fileName), content);
    } catch (IOException e) {
      throw new StorageException(AppError.STORAGE_WRITE_FAILED, e);
    }
    return fileName;
  }

  @Override
  public void delete(String path) {
    Path target = basePath.resolve(path).normalize();
    if (!target.startsWith(basePath)) {
      throw new StorageException(AppError.STORAGE_INVALID_PATH);
    }
    try {
      Files.deleteIfExists(target);
    } catch (IOException e) {
      throw new StorageException(AppError.STORAGE_DELETE_FAILED, e);
    }
  }
}
