# Flyway Migrations for the Ktor + Exposed + Postgres Server

Current state: Flyway is live. `initDatabase(config)` in
`server/src/main/kotlin/data/Database.kt` builds the Hikari pool, calls
`Database.connect`, then runs Flyway against the `public` schema with
`classpath:db/migration`. Exposed no longer creates or alters tables in
production. Migrations so far:

- `V1__Initial_schema.sql`: a `pg_dump` baseline of the schema at adoption.
- `V2__Tighten_listings_kind_to_not_null.sql`: backfills `listings.kind` and
  adds `NOT NULL`.

`server/src/main/resources/db/migration/README.md` is the step-by-step for
adding one, including the pre-commit hook that blocks schema changes without a
migration.

Based on: plusmobileapps.com Ktor + Postgres + Flyway guide and its sample repo
(github.com/plusmobileapps/ktor-postgres-flyway-sample).

## Dependencies

Versions live in `gradle/libs.versions.toml` (`flyway`, currently 10.17.0) and
`server/build.gradle.kts` consumes the catalog entries:

```kotlin
dependencies {
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
}
```

## Migration files

Location: `server/src/main/resources/db/migration/`
Naming: `V<version>__<Description>.sql` (double underscore).

Example (a later migration, so it never edits shipped history):

```sql
-- V3__Add_refresh_tokens.sql
CREATE TABLE refresh_tokens (
    id         SERIAL PRIMARY KEY,
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

Rules:
- Applied migrations are **immutable** — never edit a shipped `V*` file
  (Flyway checksums them and will refuse to run). Add a new version instead.
- Keep the Exposed table objects in sync manually — Exposed defines what the
  code *expects*; Flyway defines what the DB *is*.

## How migrations run at startup

Flyway runs against the raw `DataSource` and doesn't touch Exposed (whose
imports are `org.jetbrains.exposed.v1.jdbc.*`). The real code, in order:

```kotlin
val dataSource = HikariDataSource(hikariConfig)
Database.connect(dataSource)

Flyway
    .configure()
    .dataSource(dataSource)
    .schemas("public")
    .locations("classpath:db/migration")
    .load()
    .migrate()
```

`migrate()` throws `FlywayException` on failure and nothing catches it, so a
bad migration stops the boot. Keep it that way: a server running against a
half-migrated schema produces confusing downstream errors. The whole function
returns early, with no migration, when `DATABASE_URL` is blank or
`ci-placeholder`.

## Adopting Flyway on an existing database

Done already, kept for context. The tables pre-dated Flyway, so `V1` was
written as a `pg_dump` of the live schema. `Database.kt` does not set
`baselineOnMigrate`, so a populated database that has no
`flyway_schema_history` table needs a one-off `flyway baseline` (or a rebuild
from migrations in staging or local) before the app will start against it.

## Tests

The current server tests do not use Flyway. They connect to an in-memory H2
database and call `SchemaUtils.create(...)` for the tables they need (for
example `initListingTestDatabase()` in `ListingTestFixtures.kt`). To make tests
exercise the real schema instead, run the migrations against H2:

```kotlin
val flyway = Flyway.configure()
    .dataSource(h2DataSource)
    .cleanDisabled(false)     // allow flyway.clean() in teardown
    .load()
flyway.migrate()
// teardown: flyway.clean()
```

If migrations use Postgres-specific SQL that H2 chokes on, switch tests to
Testcontainers (`postgres:16`).

## Fat jar note (Render deploys)

Flyway had classpath-scanning issues when run from a plain jar; use the Ktor
Gradle plugin's `buildFatJar` task and run `java -jar build/libs/<app>-all.jar`
— migrations resolve correctly from the fat jar.

## Verifying migration state

```sql
select version, description, success, installed_on
from flyway_schema_history order by installed_rank;
```
