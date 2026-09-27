-- BankFlow initial schema.
-- Money is DECIMAL(19,4) throughout. InnoDB is required: the transfer path
-- relies on row-level locking via SELECT ... FOR UPDATE.

CREATE TABLE roles (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    first_name            VARCHAR(60)  NOT NULL,
    last_name             VARCHAR(60)  NOT NULL,
    email                 VARCHAR(160) NOT NULL,
    phone                 VARCHAR(15)  NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    enabled               BIT(1)       NOT NULL DEFAULT b'1',
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          DATETIME(6)  NULL,
    kyc_status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    address_line          VARCHAR(200) NULL,
    city                  VARCHAR(80)  NULL,
    state                 VARCHAR(80)  NULL,
    pincode               VARCHAR(10)  NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone)
) ENGINE = InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB;

CREATE TABLE accounts (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20)    NOT NULL,
    user_id        BIGINT         NOT NULL,
    type           VARCHAR(16)    NOT NULL,
    status         VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE',
    balance        DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    currency       VARCHAR(3)     NOT NULL DEFAULT 'INR',
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_accounts_balance_non_negative CHECK (balance >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_accounts_user ON accounts (user_id);
CREATE INDEX idx_accounts_status ON accounts (status);

CREATE TABLE beneficiaries (
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    owner_user_id              BIGINT       NOT NULL,
    beneficiary_account_number VARCHAR(20)  NOT NULL,
    beneficiary_name           VARCHAR(120) NOT NULL,
    nickname                   VARCHAR(60)  NULL,
    bank_ifsc                  VARCHAR(11)  NULL,
    verified                   BIT(1)       NOT NULL DEFAULT b'0',
    created_at                 DATETIME(6)  NOT NULL,
    updated_at                 DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_beneficiary_owner_account UNIQUE (owner_user_id, beneficiary_account_number),
    CONSTRAINT fk_beneficiaries_owner FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_beneficiaries_owner ON beneficiaries (owner_user_id);

CREATE TABLE transactions (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    reference       VARCHAR(32)    NOT NULL,
    idempotency_key VARCHAR(80)    NOT NULL,
    type            VARCHAR(16)    NOT NULL,
    status          VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    amount          DECIMAL(19, 4) NOT NULL,
    from_account_id BIGINT         NULL,
    to_account_id   BIGINT         NULL,
    balance_after   DECIMAL(19, 4) NULL,
    description     VARCHAR(255)   NULL,
    flag_reason     VARCHAR(255)   NULL,
    failure_reason  VARCHAR(255)   NULL,
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_txn_reference UNIQUE (reference),
    CONSTRAINT uk_txn_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_txn_from_account FOREIGN KEY (from_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_txn_to_account FOREIGN KEY (to_account_id) REFERENCES accounts (id),
    CONSTRAINT ck_txn_amount_positive CHECK (amount > 0),
    -- Every type must name at least one side of the movement.
    CONSTRAINT ck_txn_has_account CHECK (from_account_id IS NOT NULL OR to_account_id IS NOT NULL)
) ENGINE = InnoDB;

CREATE INDEX idx_txn_from_created ON transactions (from_account_id, created_at);
CREATE INDEX idx_txn_to_created ON transactions (to_account_id, created_at);
CREATE INDEX idx_txn_status ON transactions (status);
CREATE INDEX idx_txn_created ON transactions (created_at);

CREATE TABLE audit_logs (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT      NULL,
    action        VARCHAR(64) NOT NULL,
    entity_type   VARCHAR(48) NOT NULL,
    entity_id     VARCHAR(64) NULL,
    details       TEXT        NULL,
    ip_address    VARCHAR(45) NULL,
    user_agent    VARCHAR(255) NULL,
    event_id      VARCHAR(64) NULL,
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_audit_event_id UNIQUE (event_id)
) ENGINE = InnoDB;

CREATE INDEX idx_audit_actor ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs (created_at);

CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(120) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    channel    VARCHAR(16)  NOT NULL DEFAULT 'IN_APP',
    read_flag  BIT(1)       NOT NULL DEFAULT b'0',
    event_id   VARCHAR(64)  NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_event_id UNIQUE (event_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_notifications_user_read ON notifications (user_id, read_flag);
CREATE INDEX idx_notifications_created ON notifications (created_at);
