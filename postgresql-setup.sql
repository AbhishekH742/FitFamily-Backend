-- ===================================================================
-- FitFamily Backend - PostgreSQL Initial Setup Script
-- ===================================================================
-- This script sets up the PostgreSQL database for FitFamily Backend
-- Run this after creating the database but before starting the app
-- ===================================================================

-- ===================================================================
-- 1. EXTENSIONS
-- ===================================================================

-- Enable UUID generation (required for UUID primary keys)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Test UUID generation
SELECT gen_random_uuid() AS test_uuid;

-- ===================================================================
-- 2. PERFORMANCE INDEXES
-- ===================================================================
-- These indexes significantly improve query performance
-- Run these AFTER the application creates the initial schema
-- ===================================================================

-- User Indexes
-- ------------
-- Email lookup for login (most frequent query)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Family membership queries
CREATE INDEX IF NOT EXISTS idx_users_family_id ON users(family_id);

-- Combined index for family queries with user details
CREATE INDEX IF NOT EXISTS idx_users_family_role ON users(family_id, role);


-- Family Indexes
-- --------------
-- Join code lookup (when users join families)
CREATE INDEX IF NOT EXISTS idx_families_join_code ON families(join_code);


-- Food Indexes
-- ------------
-- Food search queries (most frequent in food search)
CREATE INDEX IF NOT EXISTS idx_foods_name ON foods(LOWER(name));

-- Food name pattern matching (for case-insensitive search)
CREATE INDEX IF NOT EXISTS idx_foods_name_trgm ON foods USING gin(name gin_trgm_ops);
-- Note: Requires extension: CREATE EXTENSION IF NOT EXISTS pg_trgm;


-- Food Portion Indexes
-- --------------------
-- Food portions lookup by food
CREATE INDEX IF NOT EXISTS idx_food_portions_food_id ON food_portions(food_id);


-- Food Log Indexes (CRITICAL FOR PERFORMANCE!)
-- ---------------------------------------------
-- User's daily logs (dashboard query - most frequent!)
CREATE INDEX IF NOT EXISTS idx_food_logs_user_date ON food_logs(user_id, date DESC);

-- Family daily logs (family dashboard)
CREATE INDEX IF NOT EXISTS idx_food_logs_family_date ON food_logs(family_id, date DESC);

-- Individual foreign keys (for joins)
CREATE INDEX IF NOT EXISTS idx_food_logs_user_id ON food_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_food_logs_family_id ON food_logs(family_id);
CREATE INDEX IF NOT EXISTS idx_food_logs_food_id ON food_logs(food_id);
CREATE INDEX IF NOT EXISTS idx_food_logs_portion_id ON food_logs(portion_id);

-- Date-based queries (analytics, reports)
CREATE INDEX IF NOT EXISTS idx_food_logs_date ON food_logs(date DESC);

-- Meal type analysis
CREATE INDEX IF NOT EXISTS idx_food_logs_meal_type ON food_logs(meal_type);

-- Combined index for meal analysis by user
CREATE INDEX IF NOT EXISTS idx_food_logs_user_meal_date ON food_logs(user_id, meal_type, date DESC);


-- ===================================================================
-- 3. CONSTRAINTS (Optional but Recommended)
-- ===================================================================

-- Case-insensitive unique email
-- This ensures users can't register with different case variations
-- Example: prevents both "john@example.com" and "John@Example.com"
ALTER TABLE users 
  DROP CONSTRAINT IF EXISTS users_email_lower_unique;

ALTER TABLE users 
  ADD CONSTRAINT users_email_lower_unique 
  UNIQUE (LOWER(email));


-- ===================================================================
-- 4. VERIFY SETUP
-- ===================================================================

-- List all indexes
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

-- List all tables
SELECT 
    table_name,
    pg_size_pretty(pg_total_relation_size(quote_ident(table_name))) AS size
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- Verify foreign key constraints
SELECT
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema = 'public'
ORDER BY tc.table_name;


-- ===================================================================
-- 5. MAINTENANCE QUERIES (Run Periodically)
-- ===================================================================

-- Update table statistics (improves query planning)
-- Run this weekly or after large data imports
ANALYZE;

-- Vacuum tables (reclaim space, update statistics)
-- Run this weekly
VACUUM ANALYZE;

-- Find unused indexes (candidates for removal)
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan AS times_used,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexname NOT LIKE '%_pkey'
  AND schemaname = 'public'
ORDER BY pg_relation_size(indexrelid) DESC;

-- Find missing indexes (tables scanned sequentially often)
SELECT
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    seq_tup_read / seq_scan AS avg_seq_read
FROM pg_stat_user_tables
WHERE seq_scan > 0
  AND schemaname = 'public'
ORDER BY seq_tup_read DESC
LIMIT 10;


-- ===================================================================
-- 6. BACKUP COMMANDS (Documentation)
-- ===================================================================

-- Backup database
-- Run this from shell (not SQL):
-- pg_dump -U fitfamily_user -d fitfamily -F c -f fitfamily_backup.dump

-- Restore database
-- Run this from shell (not SQL):
-- pg_restore -U fitfamily_user -d fitfamily fitfamily_backup.dump

-- Backup specific tables
-- pg_dump -U fitfamily_user -d fitfamily -t users -t families -F c -f fitfamily_users_backup.dump


-- ===================================================================
-- NOTES
-- ===================================================================
-- 
-- How to run this script:
-- ----------------------
-- psql -U fitfamily_user -d fitfamily -f postgresql-setup.sql
-- 
-- Or from psql prompt:
-- \i postgresql-setup.sql
-- 
-- When to run:
-- -----------
-- 1. After creating the database
-- 2. After the application creates the initial schema (run once with ddl-auto: update)
-- 3. Before switching to ddl-auto: validate
-- 
-- Index Strategy:
-- --------------
-- - Single column indexes: For simple lookups
-- - Composite indexes: For queries with multiple WHERE conditions
-- - Descending indexes: For ORDER BY DESC queries (date-based)
-- 
-- Maintenance:
-- -----------
-- - Run ANALYZE weekly
-- - Run VACUUM ANALYZE monthly
-- - Monitor slow queries with pg_stat_statements
-- - Review and remove unused indexes quarterly
-- 
-- Performance Monitoring:
-- ----------------------
-- Enable query logging in postgresql.conf:
-- log_min_duration_statement = 1000  # Log queries > 1 second
-- log_statement = 'all'  # Or 'ddl' for schema changes only
-- 
-- ===================================================================

\echo 'PostgreSQL setup script completed!'
\echo 'Check output above for any errors.'
\echo ''
\echo 'Next steps:'
\echo '1. Start your application with ddl-auto: update (first time only)'
\echo '2. Verify all tables were created: \dt'
\echo '3. Run the index creation queries above if not already run'
\echo '4. Switch to ddl-auto: validate for production'
\echo '5. Test your application'

