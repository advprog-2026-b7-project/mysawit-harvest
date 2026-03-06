-- Initialize mysawit-harvest database schema
-- This script runs automatically when the PostgreSQL container starts
-- 
-- Database per Service Pattern:
-- Each microservice manages its own database schema independently

-- Create schema for harvest service (optional, for better organization)
CREATE SCHEMA IF NOT EXISTS harvest;

-- Tables will be created automatically by Hibernate based on JPA entities
-- This script can be extended with:
-- - Initial data seeding
-- - Index creation
-- - Custom functions
-- - Partition configuration

-- Example: Create harvests table structure (will also be created by Hibernate)
-- Keeping this for documentation purposes
-- CREATE TABLE IF NOT EXISTS harvests (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     plantation_id VARCHAR(255) NOT NULL,
--     buruh_id VARCHAR(255) NOT NULL,
--     weight_kg DECIMAL(15, 2) NOT NULL,
--     description TEXT NOT NULL,
--     status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
--     rejection_reason TEXT,
--     approved_at TIMESTAMP,
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT chk_weight_positive CHECK (weight_kg > 0),
--     CONSTRAINT chk_valid_status CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED'))
-- );

-- Example: Create harvest_photos table (will also be created by Hibernate)
-- CREATE TABLE IF NOT EXISTS harvest_photos (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     harvest_id UUID NOT NULL REFERENCES harvests(id) ON DELETE CASCADE,
--     photo_url TEXT NOT NULL,
--     CONSTRAINT fk_harvest_photos_harvest FOREIGN KEY (harvest_id) REFERENCES harvests(id)
-- );

-- Seed initial data if needed
-- INSERT INTO harvests (plantation_id, buruh_id, weight_kg, description, status) 
-- VALUES 
--     ('PLANT-001', 'BURUH-001', 100.50, 'Initial harvest', 'APPROVED')
-- ON CONFLICT DO NOTHING;
