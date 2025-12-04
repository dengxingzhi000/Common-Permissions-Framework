-- ======================================================================
-- WebAuthn凭证表 DDL
-- ======================================================================
-- 参考标准：
-- - W3C Web Authentication API Level 2
-- - FIDO2 CTAP2 Protocol
-- - Google Passkey Implementation
--
-- 安全特性：
-- - 凭证ID全局唯一，防止凭证碰撞
-- - 签名计数器防重放攻击
-- - 公钥隔离存储，降低泄露风险
-- - 支持多设备绑定，提升用户体验
-- ======================================================================

CREATE TABLE IF NOT EXISTS webauthn_credential (
                                                   id UUID PRIMARY KEY,
                                                   credential_id VARCHAR(512) NOT NULL UNIQUE
    CONSTRAINT chk_credential_id_not_empty CHECK (credential_id <> ''),
    user_id UUID NOT NULL,
    public_key_pem TEXT NOT NULL
    CONSTRAINT chk_public_key_not_empty CHECK (public_key_pem <> ''),
    alg VARCHAR(64) NOT NULL
    CONSTRAINT chk_alg_valid CHECK (alg IN ('RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'EdDSA')),
    sign_count BIGINT NOT NULL DEFAULT 0
    CONSTRAINT chk_sign_count_positive CHECK (sign_count >= 0),

    -- 设备和认证器信息
    device_name VARCHAR(128),
    aaguid VARCHAR(36),
    transports TEXT[],

    -- 状态和审计
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,

    created_time TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_time TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- 索引优化
CREATE INDEX idx_webauthn_credential_user_active
    ON webauthn_credential(user_id, is_active);
CREATE INDEX idx_webauthn_credential_last_used
    ON webauthn_credential(last_used_at DESC NULLS LAST)
    WHERE is_active = TRUE;

-- 表和列注释
COMMENT ON TABLE webauthn_credential IS 'WebAuthn凭证表 - 业务外键: user_id -> sys_user.user_id';
COMMENT ON COLUMN webauthn_credential.credential_id IS 'WebAuthn凭证ID(base64url编码),全局唯一';
COMMENT ON COLUMN webauthn_credential.user_id IS '用户ID - 业务外键,由应用层保证完整性';
COMMENT ON COLUMN webauthn_credential.public_key_pem IS '公钥(PEM格式)';
COMMENT ON COLUMN webauthn_credential.alg IS '签名算法';
COMMENT ON COLUMN webauthn_credential.sign_count IS '签名计数器,防重放攻击,单调递增';
COMMENT ON COLUMN webauthn_credential.device_name IS '设备名称(用户自定义)';
COMMENT ON COLUMN webauthn_credential.aaguid IS 'Authenticator Attestation GUID,识别认证器型号';
COMMENT ON COLUMN webauthn_credential.transports IS '支持的传输方式数组,如: {usb,nfc}';
COMMENT ON COLUMN webauthn_credential.is_active IS '是否启用';
COMMENT ON COLUMN webauthn_credential.last_used_at IS '最后使用时间';

-- ======================================================================
-- 列注释
-- ======================================================================

ALTER TABLE webauthn_credential COMMENT = 'WebAuthn凭证表，符合FIDO2和W3C WebAuthn标准';

-- ======================================================================
-- 示例数据插入（可选，用于测试）
-- ======================================================================

-- INSERT INTO webauthn_credential (
--     id, credential_id, user_id, public_key_pem, alg, sign_count,
--     device_name, aaguid, transports, authenticator_attachment,
--     is_active
-- ) VALUES (
--     '550e8400-e29b-41d4-a716-446655440000',
--     'KSjKz3HHnUhFIAoS4RFCw',
--     'user_123456',
--     '-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...\n-----END PUBLIC KEY-----',
--     'ES256',
--     0,
--     'MacBook Pro TouchID',
--     '08987058-cadc-4b81-b6e1-30de50dcbe96',
--     'internal',
--     'platform',
--     TRUE
-- );

-- ======================================================================
-- 维护建议
-- ======================================================================
-- 1. 定期清理长期未使用的凭证（建议90天）
-- 2. 监控签名计数器异常（可能的克隆攻击）
-- 3. 定期备份公钥数据
-- 4. 审计凭证注册和使用日志
-- ======================================================================
