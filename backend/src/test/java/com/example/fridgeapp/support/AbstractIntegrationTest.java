package com.example.fridgeapp.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 統合テスト（@SpringBootTest でアプリケーションコンテキストを起動するテスト）の基底クラス。
 *
 * <p>テスト実行時に Testcontainers が使い捨ての PostgreSQL コンテナを起動し、{@link ServiceConnection} により DataSource /
 * Flyway の接続情報を自動注入する。これにより本番 {@code .env} や共有 DB に依存せずテストが完結する。
 *
 * <p>コンテナは static フィールドで一度だけ起動し、全テストクラスで再利用する（JVM 終了時に Testcontainers が 自動的に破棄）。実行には Docker
 * が起動している必要がある。
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  static {
    POSTGRES.start();
  }
}
