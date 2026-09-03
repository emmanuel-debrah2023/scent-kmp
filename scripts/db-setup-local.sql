-- Local development database setup for Scent
-- Fixes Flyway schema issues and grants proper permissions
-- Run as: psql -U postgres -f scripts/db-setup-local.sql

\echo '=== Scent Local Database Setup ==='

-- Ensure we're working with fragrances_dev
\c fragrances_dev postgres

-- Issue 1: Clean up garbage schema from prior misconfigured Flyway run
-- Flyway without explicit schema may have created a literal "$user", public schema
\echo 'Cleaning up garbage schema if present...'
DROP SCHEMA IF EXISTS "$user", public CASCADE;

-- Recreate public schema if it was removed
\echo 'Ensuring public schema exists...'
CREATE SCHEMA IF NOT EXISTS public;

-- Issue 2: Grant proper permissions to fragrance_user on public schema
\echo 'Granting CREATE and USAGE privileges to fragrance_user on public schema...'
GRANT USAGE ON SCHEMA public TO fragrance_user;
GRANT CREATE ON SCHEMA public TO fragrance_user;

-- Verify: check that public schema is empty (no user tables beyond system tables)
\echo 'Verifying public schema is clean for migrations...'
SELECT
    COUNT(*) AS user_table_count
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name NOT IN ('flyway_schema_history');

\echo ''
\echo 'Setup complete. Public schema is ready for Flyway migrations.'
\echo 'Permissions for fragrance_user:'
SELECT grantee, privilege_type
FROM information_schema.role_table_grants
WHERE table_schema = 'public'
  AND grantee = 'fragrance_user'
ORDER BY privilege_type;
