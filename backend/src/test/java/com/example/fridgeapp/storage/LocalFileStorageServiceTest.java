package com.example.fridgeapp.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.common.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageServiceTest {

  @TempDir private Path tempDir;

  private LocalFileStorageService storageService;

  @BeforeEach
  void setUp() {
    AppProperties appProperties =
        new AppProperties(
            null, null, null, null, new AppProperties.Storage("local", tempDir.toString()));
    storageService = new LocalFileStorageService(appProperties);
  }

  @Test
  void storeWritesFileUnderBasePathAndReturnsPathWithExtension() throws Exception {
    byte[] content = "dummy-image-bytes".getBytes();

    String path = storageService.store(content, "jpg");

    assertThat(path).endsWith(".jpg");
    Path stored = tempDir.resolve(path);
    assertThat(stored).exists();
    assertThat(Files.readAllBytes(stored)).isEqualTo(content);
  }

  @Test
  void loadReturnsStoredContent() {
    byte[] content = "dummy-image-bytes".getBytes();
    String path = storageService.store(content, "jpg");

    assertThat(storageService.load(path)).contains(content);
  }

  @Test
  void loadReturnsEmptyWhenFileDoesNotExist() {
    assertThat(storageService.load("does-not-exist.jpg")).isEmpty();
  }

  @Test
  void loadRejectsPathTraversalAttempt() {
    assertThatThrownBy(() -> storageService.load("../outside.jpg"))
        .isInstanceOf(StorageException.class)
        .extracting("error")
        .isEqualTo(AppError.STORAGE_INVALID_PATH);
  }

  @Test
  void deleteRemovesStoredFile() {
    String path = storageService.store("content".getBytes(), "png");

    storageService.delete(path);

    assertThat(tempDir.resolve(path)).doesNotExist();
  }

  @Test
  void deleteIsIdempotentWhenFileDoesNotExist() {
    storageService.delete("does-not-exist.jpg");
  }

  @Test
  void deleteRejectsPathTraversalAttempt() {
    assertThatThrownBy(() -> storageService.delete("../outside.jpg"))
        .isInstanceOf(StorageException.class)
        .extracting("error")
        .isEqualTo(AppError.STORAGE_INVALID_PATH);
  }
}
