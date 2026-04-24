CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT REFERENCES users(id),
    operator_username_snapshot VARCHAR(50),
    operator_role_snapshot VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(20) NOT NULL
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'ROLLBACK', 'LOGIN', 'LOGIN_FAIL')),
    before_value JSONB,
    after_value JSONB,
    ip_address VARCHAR(45),
    description TEXT
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_logs_operator_time ON audit_logs(operator_id, created_at DESC);
CREATE INDEX idx_audit_logs_action_time ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_logs_time ON audit_logs(created_at DESC);
