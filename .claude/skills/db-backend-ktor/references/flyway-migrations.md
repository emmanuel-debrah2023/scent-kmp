# Flyway Migrations for the Ktor + Exposed + Postgres Server

Current state: the server still uses Exposed's
`SchemaUtils.createMissingTablesAndColumns()` on boot (`data/Database.kt`). The
schema is already non-trivial — users, fragrances, media, posts, listings,
listing media, orders, reviews — and `createMissingTablesAndColumns` can't
rename, drop, backfill, or change types, and gives no history of what changed.
Several tickets are blocked on this (`chore/introduce-flyway`,
`chore/tighten-listing-fill-columns` — the listing fill columns are nullable
only because boot-time DDL can't add `NOT NULL`). This is overdue, not
hypothetical.

Based on: plusmobileapps.com Ktor + Postgres + Flyway guide and its sample repo
(github.com/plusmobileapps/ktor-postgres-flyway-sample).

Pin `flyway-core` and `flyway-database-postgresql` in `gradle/libs.versions.toml`
alongside the other versions rather than a local `val` — the snippets below use
an inline version for readability only.

## Dependencies (`server/build.gradle.kts`)

```kotlin
val flywayVersion = "10.12.0"

buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("org.flywaydb:flyway-database-postgresql:$flywayVersion") }
}

dependencies {
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
}
```

## Migration files

Location: `server/src/main/resources/db/migration/`
Naming: `V<version>__<Description>.sql` (double underscore).

```sql
-- V1__Create_users_table.sql
CREATE TABLE users (
    id             SERIAL PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(100) NOT NULL UNIQUE,
    password_hash  VARCHAR(255),
    google_id      VARCHAR(255) UNIQUE,
    apple_id       VARCHAR(255) UNIQUE,
    display_name   VARCHAR(100) NOT NULL,
    avatar_url     VARCHAR(255),
    bio            TEXT,
    is_seller      BOOLEAN NOT NULL DEFAULT FALSE,
    follower_count INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL
);
```

```sql
-- V2__Add_refresh_tokens.sql
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

## Run migrations at startup (replaces `createMissingTablesAndColumns`)

`Database.connect` / `transaction` imports are Exposed 1.x
(`org.jetbrains.exposed.v1.jdbc.*`). Flyway runs against the raw `DataSource`
and doesn't touch Exposed.

```kotlin
private fun migrate(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()          // throws FlywayException on failure — let it crash the boot
}

// order matters: migrate BEFORE Database.connect / any Exposed usage
init {
    val ds = hikariDataSource()
    migrate(ds)
    Database.connect(ds)
}
```

Let migration failures crash startup rather than logging and continuing — a
server running against a half-migrated schema produces confusing downstream
errors.

## Adopting Flyway on an existing (already-populated) database

The tables already exist from `createMissingTablesAndColumns`, so a plain V1
`CREATE TABLE` will fail. Options:

1. Write `V1` to match the current schema exactly, then baseline:
   `.baselineOnMigrate(true).baselineVersion("1")` — Flyway records V1 as
   applied without running it on existing DBs, while fresh DBs run it.
2. Or (staging/local only) drop and recreate from migrations.

## Tests

Reuse migrations in the H2 test database so tests exercise the real schema:

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
