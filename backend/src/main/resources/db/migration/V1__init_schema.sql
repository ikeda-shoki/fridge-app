-- =========================================================
-- V1: 初期スキーマ作成
-- =========================================================

-- users
CREATE TABLE users (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    google_sub   VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    avatar_url   TEXT,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID
);

CREATE UNIQUE INDEX uq_users_google_sub       ON users (google_sub);
CREATE UNIQUE INDEX uq_users_email_active     ON users (email) WHERE deleted_at IS NULL;

-- groups
CREATE TABLE groups (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID
);

-- group_members
CREATE TABLE group_members (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id   UUID        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users  (id) ON DELETE RESTRICT,
    role       VARCHAR(20) NOT NULL,
    invited_by UUID        REFERENCES users (id) ON DELETE SET NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    CONSTRAINT chk_group_members_role CHECK (role IN ('OWNER', 'MEMBER'))
);

CREATE UNIQUE INDEX uq_group_members_group_user ON group_members (group_id, user_id);
CREATE INDEX        idx_group_members_user_id   ON group_members (user_id);

-- invitation_codes
CREATE TABLE invitation_codes (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    code            CHAR(6)     NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    used_by         UUID        REFERENCES users (id) ON DELETE SET NULL,
    used_at         TIMESTAMPTZ,
    failed_attempts INT         NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    CONSTRAINT chk_invitation_codes_failed_attempts CHECK (failed_attempts >= 0)
);

CREATE UNIQUE INDEX uq_invitation_codes_code_active ON invitation_codes (code) WHERE used_at IS NULL;
CREATE INDEX        idx_invitation_codes_group_id   ON invitation_codes (group_id);

-- food_master
CREATE TABLE food_master (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(100) NOT NULL,
    name_kana        VARCHAR(100) NOT NULL,
    default_category VARCHAR(50),
    default_unit     VARCHAR(20),
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       UUID,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by       UUID
);

CREATE INDEX idx_food_master_name_kana ON food_master (name_kana text_pattern_ops);

-- fridge_items
CREATE TABLE fridge_items (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id       UUID           NOT NULL REFERENCES groups      (id) ON DELETE CASCADE,
    food_master_id UUID           REFERENCES food_master (id) ON DELETE SET NULL,
    display_name   VARCHAR(100)   NOT NULL,
    quantity       NUMERIC(10, 2) NOT NULL DEFAULT 1,
    unit           VARCHAR(20),
    category       VARCHAR(50),
    expires_at     DATE,
    purchased_at   DATE,
    purchased_by   UUID           REFERENCES users (id) ON DELETE SET NULL,
    image_path     TEXT,
    memo           TEXT,
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by     UUID,
    CONSTRAINT chk_fridge_items_status   CHECK (status   IN ('ACTIVE', 'CONSUMED', 'DELETED')),
    CONSTRAINT chk_fridge_items_quantity CHECK (quantity >= 0)
);

CREATE INDEX idx_fridge_items_group_status_expires ON fridge_items (group_id, status, expires_at);
CREATE INDEX idx_fridge_items_group_category       ON fridge_items (group_id, category);

-- shopping_items
CREATE TABLE shopping_items (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id       UUID           NOT NULL REFERENCES groups      (id) ON DELETE CASCADE,
    food_master_id UUID           REFERENCES food_master (id) ON DELETE SET NULL,
    display_name   VARCHAR(100)   NOT NULL,
    quantity       NUMERIC(10, 2) NOT NULL DEFAULT 1,
    memo           TEXT,
    checked        BOOLEAN        NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by     UUID,
    CONSTRAINT chk_shopping_items_quantity CHECK (quantity >= 0)
);

CREATE INDEX idx_shopping_items_group_checked ON shopping_items (group_id, checked);

-- consumption_events（履歴専用: created_at / created_by のみ）
CREATE TABLE consumption_events (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    fridge_item_id    UUID           NOT NULL REFERENCES fridge_items (id) ON DELETE RESTRICT,
    quantity_consumed NUMERIC(10, 2) NOT NULL,
    reason            VARCHAR(20)    NOT NULL,
    recipe_ref        TEXT,
    consumed_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by        UUID,
    CONSTRAINT chk_consumption_events_reason           CHECK (reason            IN ('MANUAL', 'RECIPE', 'EXPIRED')),
    CONSTRAINT chk_consumption_events_qty_consumed     CHECK (quantity_consumed >  0)
);

CREATE INDEX idx_consumption_events_fridge_item_id ON consumption_events (fridge_item_id);

-- refresh_tokens
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID
);

CREATE UNIQUE INDEX uq_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX        idx_refresh_tokens_user_id   ON refresh_tokens (user_id);
