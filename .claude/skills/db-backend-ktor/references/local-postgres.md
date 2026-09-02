# Local Postgres, Env Vars & DB Inspection

## Local database with Docker

```bash
docker run --name scent-db -e POSTGRES_PASSWORD=dev -p 5432:5432 -d postgres:16

# stop/start between sessions (data persists in the container)
docker stop scent-db && docker start scent-db

# throwaway: add --rm and drop -d to run in foreground
```

## Env vars → run the server

The server reads config through `application.conf` (HOCON) from env vars.
Export them in the *same shell* that runs Gradle. `initDatabase()` logs a
warning and skips DB init when `DATABASE_URL` is blank or `ci-placeholder`
(so the server boots for route tests without a database), but any endpoint
that hits the DB will then fail — set all four for real local work:

```bash
export JWT_SECRET=dev-secret
export DATABASE_URL="jdbc:postgresql://localhost:5432/postgres"
export DATABASE_USER=postgres
export DATABASE_PASSWORD=dev
./gradlew :server:run
```

Tip: keep a `server/.env.local` (gitignored) and load it with
`set -a; source server/.env.local; set +a`.

**JWT_SECRET gotcha:** if the secret differs between the process that issued a
token and the one verifying it (e.g. you restarted with a different export),
every token 401s with no useful error. When auth mysteriously breaks, check
this first.

## Pointing at staging (Supabase)

```bash
export DATABASE_URL="jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?sslmode=require"
```

- `?sslmode=require` is mandatory — without it connections are rejected.
- Free-tier connection limits are low; keep Hikari `maximumPoolSize` at ≤ 3
  and don't run multiple local servers against it simultaneously.
- Prefer a *separate* Supabase project (or schema) for staging vs production
  data — smoke tests insert rows.

## Inspecting the database with psql

```bash
# local docker
docker exec -it scent-db psql -U postgres

# or from host / against Supabase
psql "postgresql://postgres:dev@localhost:5432/postgres"
```

Handy queries:

```sql
\dt                                       -- list tables
\d users                                  -- describe users table
select id, email, username, created_at from users order by id desc limit 5;

-- verify passwords are hashed (must start with $2a$/$2b$, never plaintext)
select email, left(password_hash, 7) as hash_prefix from users limit 5;

-- clean up smoke-test rows
delete from users where email like 'smoke%@test.com';
```

## Connection pooling (HikariCP)

The server already runs on a pooled `HikariDataSource` — see `initDatabase()`
in `server/src/main/kotlin/data/Database.kt` (`maximumPoolSize = 3`,
`minimumIdle = 1`, 30s connection / 10min idle / 30min max-lifetime timeouts,
prepared-statement caching). Config is read from `application.conf`
(`database.url` / `.user` / `.password`), not `System.getenv` directly. When
touching pooling, edit that function rather than adding a second datasource;
keep `maximumPoolSize ≤ 3` for the Render/Supabase free tiers.

Test code is the exception — it uses a plain
`Database.connect("jdbc:h2:mem:...")` with no pool (see
`references/ktor-test-harness.md`).

## Common failure signatures

| Symptom | Cause |
|---|---|
| `Connection refused` | DB container not running / wrong port |
| `password authentication failed` | wrong `DATABASE_USER`/`DATABASE_PASSWORD` |
| `SSL error` / `connection rejected` on Supabase | missing `?sslmode=require` |
| `relation "users" does not exist` | `initDatabase()` not run, or wrong database |
| `too many connections` | pool size too big for free tier, or leaked servers |
