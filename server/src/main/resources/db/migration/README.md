# Database Migrations

All database schema changes go through Flyway migrations. This directory tracks the complete schema evolution.

## How to add a migration

### 1. Make your schema change in Kotlin

Update `Schema.kt` or `SchemaEntities.kt` in `server/src/main/kotlin/data/schema/`.

### 2. Write the migration SQL

Migrations are hand-written SQL. Exposed does not create or alter tables in
production (Flyway does), so there is nothing to generate from. Write only the
change, for example `ALTER TABLE`, `CREATE TABLE` or `CREATE INDEX`, and
backfill any rows before adding a `NOT NULL` constraint (see
`V2__Tighten_listings_kind_to_not_null.sql`).

To check the SQL, apply it to a throwaway local database that already has the
earlier migrations (see **Testing migrations locally** below) and compare it
with your Exposed table definitions. Exposed defines what the code expects;
the migration defines what the database is.

### 3. Name your migration file

Flyway migrations follow a strict naming convention:

```
V<version>__<description>.sql
```

- **V1**, **V2**, etc. — versions must be sequential integers
- Exactly **two underscores** separating version from description
- **Description**: use `snake_case` for clarity (e.g., `add_user_preferences_table`)

Example: `V2__add_user_preferences_table.sql`

### 4. Stage and commit

```bash
git add server/src/main/resources/db/migration/V<N>__<description>.sql
git commit -m "chore(db): <description of schema change>"
```

The pre-commit hook will verify you've added a migration when schema files change —
see **Install the pre-commit hook** below. It has to be installed once per clone;
`.git/hooks/` itself is never committed, so the hook script lives in `scripts/git-hooks/`
and is wired in via `git config core.hooksPath`.

## Install the pre-commit hook

Run once per clone:

```bash
git config core.hooksPath scripts/git-hooks
```

This points git at the tracked `scripts/git-hooks/` directory instead of the default,
untracked `.git/hooks/`. From then on, `git commit` runs `scripts/git-hooks/pre-commit`
automatically and blocks any commit that touches `Schema.kt` / `SchemaEntities.kt`
without a corresponding `V<N>__*.sql` migration staged alongside it.

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

## Rollback behaviour

**Flyway does not provide automatic rollbacks.** Migrations are one-way by design. Instead:

### If a migration fails on deployment

1. **Immediate**: Application will fail to start. No cleanup needed — the failed migration is recorded in `flyway_schema_history` with status `FAILED`.

2. **Fix the root cause**: If the migration is syntactically wrong, fix it in the code repository. If it's a constraint violation or data issue, investigate why.

3. **Create a new migration** to undo the failed one's side effects, OR fix the original and increment the version (e.g., rename `V2__bad_migration.sql` to `V3__bad_migration_fixed.sql` and adjust the contents).

   ```sql
   -- V3__fix_migration_v2.sql
   -- Undo whatever V2 attempted (if it partially succeeded)
   DROP TABLE IF EXISTS public.new_table;
   -- Then apply the corrected change
   CREATE TABLE public.new_table (id INT PRIMARY KEY);
   ```

### On Supabase free tier (cold starts)

Migrations run on every cold start. A failed migration will:
- Keep the database locked until manually released
- Block the Render dyno from starting
- Require SSH into Supabase to inspect `flyway_schema_history` and manually resolve

**Strategy**: Test thoroughly locally before merging. A failing migration in production is a hard stop.

### Prevention

- Always test migrations on a local copy of production schema
- Use `pg_dump -U postgres --schema-only <prod-db>` to get the exact schema
- Test with realistic data volumes (esp. for backfill operations)
- Add `EXPLAIN ANALYZE` to UPDATEs/INSERTs that modify large tables
- Keep migrations small and focused — one logical change per version

## Never modify applied migrations

Once a migration is merged to `main`:
- ❌ Do NOT edit it
- ❌ Do NOT rename it
- ✅ Create a new migration to fix issues

This keeps the history reproducible across all environments.
