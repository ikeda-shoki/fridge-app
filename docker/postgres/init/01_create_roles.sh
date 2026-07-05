#!/usr/bin/env bash
# PostgreSQL ロール初期化スクリプト
# docker-entrypoint-initdb.d により、コンテナ初回起動時に一度だけ実行される
set -euo pipefail

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" <<-SQL

  -- PUBLIC からスキーマへの CREATE 権限を剥奪
  REVOKE CREATE ON SCHEMA public FROM PUBLIC;

  -- fridgeapp_migrator: DDL + DML（Flyway 専用）
  CREATE ROLE "$DB_MIGRATOR_USER" WITH LOGIN PASSWORD '$DB_MIGRATOR_PASSWORD';
  GRANT ALL PRIVILEGES ON DATABASE "$POSTGRES_DB" TO "$DB_MIGRATOR_USER";
  GRANT ALL ON SCHEMA public TO "$DB_MIGRATOR_USER";

  -- fridgeapp_app: DML のみ（Spring Boot 実行時専用）
  CREATE ROLE "$DB_APP_USER" WITH LOGIN PASSWORD '$DB_APP_PASSWORD';
  GRANT CONNECT ON DATABASE "$POSTGRES_DB" TO "$DB_APP_USER";
  GRANT USAGE ON SCHEMA public TO "$DB_APP_USER";

  -- migrator が今後作成するテーブル・シーケンスへの権限を app に自動付与
  ALTER DEFAULT PRIVILEGES FOR ROLE "$DB_MIGRATOR_USER" IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "$DB_APP_USER";
  ALTER DEFAULT PRIVILEGES FOR ROLE "$DB_MIGRATOR_USER" IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO "$DB_APP_USER";
SQL

echo "Roles created: $DB_MIGRATOR_USER (DDL+DML), $DB_APP_USER (DML only)"
