CREATE TABLE IF NOT EXISTS harvests (
    id UUID PRIMARY KEY,
    plantation_id VARCHAR(255) NOT NULL,
    buruh_id VARCHAR(255) NOT NULL,
    buruh_name VARCHAR(255),
    weight_kg NUMERIC(15, 2) NOT NULL,
    notes TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    rejection_reason TEXT,
    approved_at TIMESTAMP,
    harvest_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_harvest_buruh_date UNIQUE (buruh_id, harvest_date)
);

CREATE TABLE IF NOT EXISTS harvest_photos (
    id UUID PRIMARY KEY,
    harvest_id UUID NOT NULL,
    photo_url TEXT NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    file_size_bytes BIGINT,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_harvest_photos_harvest
        FOREIGN KEY (harvest_id)
        REFERENCES harvests (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_harvests_buruh_date ON harvests (buruh_id, harvest_date);
CREATE INDEX IF NOT EXISTS idx_harvests_status ON harvests (status);
CREATE INDEX IF NOT EXISTS idx_harvest_photos_harvest_id ON harvest_photos (harvest_id);