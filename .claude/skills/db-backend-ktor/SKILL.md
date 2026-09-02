---
name: db-backend-ktor
description: Backend dev-tools and local testing workflow for the Scent Ktor + Exposed + PostgreSQL server (deployed on Render free tier + Supabase). Use this whenever the user wants to smoke-test or verify backend endpoints (auth or otherwise) with curl, spin up a local Postgres (Docker) or point at staging/Supabase, write or run Ktor testApplication tests (including JWT-protected routes and H2 in-memory database tests), set up or debug Flyway migrations, inspect the database with psql, debug env-var/JWT-secret mismatches, or build any repeatable dev script for the server. Trigger on phrases like "smoke test", "test the endpoints", "curl the API", "run the backend locally", "check the database", "write server tests", "migration", or any request to verify backend behavior before a deploy — even if the user doesn't say "skill" or name a tool.
---

# db-backend-ktor — Backend Dev Tools & Local Testing

Everything for verifying the Scent Ktor backend locally and against staging: curl smoke tests, local Postgres, Ktor test harness, Flyway migrations, and DB inspection.

## Project facts (assume these unless told otherwise)

- **Stack:** Ktor 3.1.1, Exposed 1.3.1, PostgreSQL, HOCON config (`application.conf`) driven by env vars. Versions are pinned in `gradle/libs.versions.toml` (`ktor`, `exposed`) — check there first.
- **Exposed 1.x namespace:** imports moved to `org.jetbrains.exposed.v1.*` (e.g. `org.jetbrains.exposed.v1.jdbc.SchemaUtils`, `org.jetbrains.exposed.v1.jdbc.transactions.transaction`). Code samples written against Exposed 0.x `org.jetbrains.exposed.sql.*` will not compile.
- **Auth:** JWT HMAC256, 24h expiry, claims `userId` (Int) + issuer `fragrances-app` + audience `fragrances-users`; BCrypt password hashes. `plugins/Security.kt` `configureSecurity()` falls back to secret `"secret"` / issuer `fragrances-app` / audience `fragrances-users` when no `jwt.*` config is present — that's why route tests can mint tokens with just `HMAC256("secret")`.
- **Endpoints:** `/api/v1/auth/{register,login,me}` plus `fragrances`, `listings` (+ `listings/{id}` PATCH/DELETE, `listings/brands`), `media` (`image-upload-url`, `{uid}/complete`), and `posts` route groups. Check `server/src/main/kotlin/routing/` for the current surface.
- **Env vars:** `JWT_SECRET`, `DATABASE_URL` (JDBC), `DATABASE_USER`, `DATABASE_PASSWORD`; media/storage adds `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_STORAGE_BUCKET`, and `IMAGE_PROVIDER` / `STREAM_PROVIDER` (`fake` for local dev). See `.env.template`.
- **Deploy:** Render free tier (cold starts 30–60s) + Supabase Postgres (`?sslmode=require`)
- **Schema:** still `SchemaUtils.createMissingTablesAndColumns()` on boot in `initDatabase(config)` (`server/src/main/kotlin/data/Database.kt`), behind a pooled `HikariDataSource`. Flyway is not adopted yet — tracked as `chore/introduce-flyway`. Boot-time DDL is skipped when `DATABASE_URL` is blank or `ci-placeholder`.
- **Android emulator** reaches the host via `http://10.0.2.2:8080`, needs cleartext traffic allowed in debug

## Workflow: pick the right lane

| Task | Do this | Reference |
|---|---|---|
| Verify auth endpoints against a running server | Run `scripts/smoke-auth.sh` | `references/curl-recipes.md` |
| Verify any other protected endpoint | `scripts/smoke-endpoint.sh METHOD PATH [BODY]` — bootstraps a user, runs auth negative-case trio | `references/curl-recipes.md` |
| Stand up a local database | Docker Postgres one-liner + env vars | `references/local-postgres.md` |
| Automated server tests (CI-able) | Ktor `testApplication` + H2 in-memory DB | `references/ktor-test-harness.md` |
| Schema changes / migrations | Flyway versioned SQL migrations | `references/flyway-migrations.md` |
| Inspect data / verify hashes | `psql` recipes in `references/local-postgres.md` | |

Read the matching reference file before writing code — they contain the exact patterns, versions, and known pitfalls for this stack.

## Core loop: smoke-testing endpoints

1. **Start the server against a known DB.** Local Docker Postgres for fast iteration; Supabase connection string for staging parity. Export all four env vars in the same shell that runs `./gradlew :server:run` — a missing var fails at config load; a *different* `JWT_SECRET` between runs makes old tokens invalid silently.
2. **Run the smoke script:** `bash scripts/smoke-auth.sh` (set `BASE` env var to target staging). It exercises register → login → /me, plus negative cases (wrong password, missing token, garbage token, duplicate email), using a timestamp-unique email so it's rerunnable.
3. **Verify beyond status codes:**
   - Token from register works on `/me` — proves signer and verifier agree on secret/issuer/audience (the classic failure mode).
   - `/me` returns *that* user's profile, not just any 200.
   - `password_hash` in the DB is a BCrypt string (`$2a$...`), never plaintext.
   - Decode the token payload (`cut -d. -f2 | base64 -d`) — `userId` present, `exp` ≈ 24h out.
4. **Against Render staging:** same script with `BASE=https://<app>.onrender.com/api/v1/auth`; add `--max-time 90` to the first request or warm the instance first (free-tier cold start).

## Writing new dev scripts

For new endpoints (posts, fragrances, listings), start with `scripts/smoke-endpoint.sh` — it already handles user bootstrap, the token, and the negative-case trio; pass `EXPECT=201` etc. for creates. Only write a bespoke script when a flow spans multiple dependent calls (e.g. create listing → purchase → verify). When you do:

- Put them in `server/scripts/` in the repo, `set -euo pipefail`, executable
- Make every run idempotent — unique identifiers per run (timestamps), never rely on manual DB cleanup
- Print `%{http_code}` with a label for negative cases; use `curl -sf` for cases that must succeed so the script fails loudly
- Parse JSON with `jq`; capture the token once and reuse
- Keep secrets out of scripts — read from env, document required vars in a header comment
- For protected routes, always include the three-token trio of negative tests: no token, expired/garbage token, valid token for a *different* user (authorization, not just authentication)

## Debugging quick hits

- **401 on /me with a fresh token** → `JWT_SECRET`/issuer/audience mismatch between `generateToken()` and the verifier config; print both sides.
- **Connection refused locally** → server not up, wrong port, or (from emulator) using `localhost` instead of `10.0.2.2`.
- **`relation "users" does not exist`** → `initDatabase()` not called before routes, or connected to the wrong database/schema.
- **Works locally, 500s on Render** → almost always a missing/typo'd env var on Render; check the dashboard against the required list above.
- **Supabase connection failures** → missing `?sslmode=require` in `DATABASE_URL`, or free-tier connection limit hit (keep Hikari `maximumPoolSize` ≤ 3).

## Useful sample repos

- Ktor `jwt-auth-tests` sample (RSA-JWT route testing patterns): github.com/ktorio/ktor-samples/tree/main/jwt-auth-tests
- Ktor `fullstack-mpp` sample (shared-module + server layout): github.com/ktorio/ktor-samples/tree/main/fullstack-mpp
- Postgres + Flyway + Exposed reference project: github.com/plusmobileapps/ktor-postgres-flyway-sample
