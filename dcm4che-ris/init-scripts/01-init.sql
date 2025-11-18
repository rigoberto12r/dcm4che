-- Initial database setup script for dcm4che-ris
-- This script runs automatically when the PostgreSQL container starts for the first time

-- Ensure database exists
CREATE DATABASE IF NOT EXISTS ris_db;

-- Connect to the database
\c ris_db;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- For text search optimization

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ris_db TO ris_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ris_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ris_user;

-- Set timezone
SET timezone = 'UTC';

-- Log completion
SELECT 'Database initialized successfully' AS status;
