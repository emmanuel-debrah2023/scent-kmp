---
paths:
  - "gradle/libs.versions.toml"
  - "**/build.gradle.kts"
  - "settings.gradle.kts"
---

# Scent — adding dependencies

Adding a dependency is a decision the user makes, not one you make on their
behalf. A `PreToolUse` hook blocks new coordinates until they're approved. When
you get blocked, present the case and wait — do not look for a path around it.

## Verify before proposing

**Never state a version, artifact coordinate, or platform-support claim from
memory.** KMP target coverage and current stable versions are exactly the facts
that go stale between model releases, and being confidently wrong here costs a
build cycle.

Use the `kmp-libraries-expert` skill / klibs.io MCP to confirm:
- the current stable version and exact coordinates
- which targets it actually publishes for
- when it was last published, and whether it looks maintained

Cite the klibs.io link when you propose it.

## Target requirements

| Module | Must support |
|---|---|
| `shared` (commonMain) | `androidTarget`, `iosArm64`, `iosSimulatorArm64`, `iosX64` |
| `composeApp` (commonMain) | same, plus Compose Multiplatform compatibility |
| `shared` androidMain / iosMain | that platform only — this is the right home for a platform-specific library |
| `server` | JVM only |

A library missing an iOS target does not go in `commonMain`. Either find one
that has it, or put it behind an `expect`/`actual` with a platform-specific
implementation on each side — and say which you're proposing.

## Architectural fit (ADS-STE100)

A good library and a Scent-compatible library are not the same thing. Reject or
flag anything that:

- **Is a DI framework other than Koin.** Koin is the contract. No Hilt, no Kodein,
  no manual service locator.
- **Is a navigation library.** State-based per-tab navigation is deliberate until
  official Compose Multiplatform Navigation stabilises. See `ads-navigation.md`.
- **Replaces kotlinx.serialization**, or requires its own serialization layer for
  DTOs.
- **Replaces Ktor.** Ktor client is the HTTP layer on both platforms and Ktor
  server is the backend. Don't add Retrofit/OkHttp into `shared`.
- **Forces exceptions past the repository boundary.** A library that throws is
  fine — repositories catch at the platform edge and re-wrap into
  `AppError`. A library whose *public API contract* is exception-based
  control flow across many call sites is not.
- **Requires reflection or codegen that breaks on Kotlin/Native.** Common failure
  mode for JVM-first libraries with a nominal KMP artifact.

## Server module — Render free tier

The backend runs on Render's free tier with Supabase Postgres. That constrains
things:

- **No in-memory session or cache stores.** Cold starts wipe them; this is the
  original reason auth is stateless JWT rather than server sessions
  (`docs/auth.md`).
- **Watch startup cost.** Java agents, heavy reflection scanning, and large
  framework initialisation make cold starts worse on a container that sleeps.
- **No dependency that assumes persistent local disk.**

## Mechanics

- **Check `gradle/libs.versions.toml` first.** The dependency you want may
  already be there. Adding a second HTTP client or JSON library because you
  didn't look is the most common version of this mistake.
- **Version catalog only.** New coordinates go in `libs.versions.toml` and are
  referenced as `implementation(libs.some.lib)`. Never inline
  `implementation("group:artifact:version")` in a `build.gradle.kts`.
- **Align versions.** Ktor client, Ktor server, and the serialization plugin move
  together — bumping one in isolation breaks the others.
- **Check the licence.** Anything not permissive (Apache-2.0, MIT, BSD) gets
  raised explicitly before it goes in.

## What to say when proposing one

Give the user: what it does, the klibs.io link, current stable version, target
coverage, which module it lands in, what it replaces or saves writing, and the
one thing that might make them say no (maintenance status, licence, size, an
ADS-STE100 rough edge). Then stop and let them decide.
