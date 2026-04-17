CREATE TABLE IF NOT EXISTS buruh_mandor_assignments (
    buruh_id VARCHAR(255) PRIMARY KEY,
    mandor_id VARCHAR(255) NOT NULL,
    buruh_name VARCHAR(255),
    plantation_id VARCHAR(255),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_buruh_mandor_assignments_mandor_id
    ON buruh_mandor_assignments (mandor_id);