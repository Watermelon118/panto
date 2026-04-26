CREATE TABLE system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(50) NOT NULL UNIQUE,
    setting_value VARCHAR(200) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_system_settings_key ON system_settings(setting_key);

INSERT INTO system_settings (setting_key, setting_value)
SELECT 'expiry_warning_days', '30'
WHERE NOT EXISTS (
    SELECT 1
    FROM system_settings
    WHERE setting_key = 'expiry_warning_days'
);
