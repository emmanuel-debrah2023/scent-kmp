# Database Migrations

All database schema changes go through Flyway migrations. This directory tracks the complete schema evolution.

## How to add a migration

### 1. Make your schema change in Kotlin

Update `Schema.kt` or `SchemaEntities.kt` in `server/src/main/kotlin/data/schema/`.

### 2. Generate the migration file

Create a throwaway test database and apply your changes:

```bash
# Create a temporary database for schema testing
psql -U postgres -c "CREATE DATABASE temp_schema_test;"

# Run your app or a test that triggers SchemaUtils.createMissingTablesAndColumns()
# against this database to generate the full new schema

# Dump the schema
pg_dump -U postgres --schema-only temp_schema_test > migration.sql

# Clean up
psql -U postgres -c "DROP DATABASE temp_schema_test;"
```

### 3. Name your migration file

Flyway migrations follow a strict naming convention:

```
V<version>__<description>.sql
```

- **V1**, **V2**, etc. — versions must be sequential integers
- Exactly **two underscores** separating version from description
- **Description**: use `snake_case` for clarity (e.g., `add_user_preferences_table`)

Example: `V2__add_user_preferences_table.sql`

### 4. Edit the migration file

The pg_dump output includes database-specific metadata and comments. Keep the essential SQL:
- CREATE TABLE statements
- ALTER TABLE statements for constraints
- CREATE INDEX statements (if needed)

Remove:
- PostgreSQL comments like `-- Dumped by pg_dump`
- SET statements (these are env-specific)
- Comments about owners (e.g., `ALTER TABLE public.users OWNER TO postgres`)

### 5. Stage and commit

```bash
git add server/src/main/resources/db/migration/V<N>__<description>.sql
git commit -m "chore(db): <description of schema change>"
```

The pre-commit hook will verify you've added a migration when schema files change.

## Flyway in production

- Flyway tracks applied migrations in the `flyway_schema_history` table
- Each migration runs exactly once
- Migrations run in version order on startup
- Failed migrations block the application from starting (fail-fast)

## Testing migrations locally

To test a migration against your local database:

```bash
# Reset your local database to a known state
dropdb fragrances_dev
createdb fragrances_dev

# Run the app — it will apply all migrations from scratch
./gradlew :server:run
```

## Never modify applied migrations

Once a migration is merged to `main`:
- ❌ Do NOT edit it
- ❌ Do NOT rename it
- ✅ Create a new migration to fix issues

This keeps the history reproducible across all environments.
