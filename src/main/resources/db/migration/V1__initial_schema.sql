CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(72) NOT NULL,
    email_verified BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE verification_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT(1) NOT NULL DEFAULT b'0',
    CONSTRAINT pk_verification_tokens PRIMARY KEY (id),
    CONSTRAINT uq_verification_tokens_token UNIQUE (token),
    CONSTRAINT uq_verification_tokens_user_id UNIQUE (user_id),
    CONSTRAINT fk_vt_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_vt_token ON verification_tokens (token);
CREATE INDEX idx_vt_user ON verification_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT(1) NOT NULL DEFAULT b'0',
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_prt_token ON password_reset_tokens (token);
CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);
