-- V3: users テーブルから email 列を削除（未使用 PII の最小化）
DROP INDEX uq_users_email_active;
ALTER TABLE users DROP COLUMN email;
