# ADR-0003: Hand-roll the A2UI client engine in `:shared`; do not adopt a renderer library

- **Status:** Accepted
- **Date:** 2026-10-18
- **Deciders:** Emmanuel
- **Ticket:** Scent Tasks Tracker, "A2UI client engine: hand-rolled scent/v1 surface reducer, JSON Pointer and action builder (PR 1 of 2)" (branch `chore/a2ui-commonmain-renderer`)

## Context

The ask-back flow is server driven. The server sends A2UI v0.9 messages (`createSurface`,
`updateComponents`, `updateDataModel`, `deleteSurface`) built from the `scent/v1` catalog, and the app
shows them. The message types and the seven catalog components already live in `:contracts` and are the
single source of truth for both sides (PR #122). The question was whether the client should adopt an
existing Compose Multiplatform A2UI renderer or build a small engine of its own.

Checked on 2026-10-18 (GitHub, Maven Central, each project's docs; no build spike was run):

- **a2ui-4k 0.10.0** (Apache-2.0): targets Android, iOS and JVM. It is built on Kotlin 2.1.20 and
  Compose Multiplatform 1.9.3 (Scent is on 2.3.21 and 1.10.2), its Android target is JVM 21 while
  `composeApp` is JVM 11, and it adds Kermit and Coil. Its custom-catalog API gives untyped component
  properties, so all seven `scent/v1` composables would still be written by hand, and `:contracts`
  messages would have to be re-encoded to JSON for it to parse again.
- **A2CUI 0.1.0-a02:** the repository is archived and the alpha forces newer serialization.
- **androidx.a2ui 1.0.0-alpha01:** Android only, so it cannot serve `commonMain`.
- **Official Kotlin SDK:** JVM only, pulls in Google ADK.

## Decision

Build the engine ourselves, in `:shared`, over the `:contracts` types:

- `JsonPointer.read` and `JsonPointer.write` in `:contracts` (RFC 6901, with `""` and `"/"` both meaning
  the root of the data model, as A2UI does).
- Non-null domain types (`Surface`, `SurfaceNode`, `SurfaceMessage`) and a mapper from the nullable DTOs
  (ADS-STE100 null safety). A component that cannot be shown becomes `SurfaceNode.Unsupported`.
- `SurfaceReducer`: a batch is all or nothing; a rejected batch leaves the state untouched; nothing
  throws. A duplicate `createSurface` is an error, as the v0.9 spec says.
- `SurfaceActionBuilder`: resolves an action's context bindings from the local data model; the name must
  be in the `ScentAction` allowlist.
- Rendering (`ScentSurface`) is a separate PR and is plain Compose over the existing component library.

## Consequences

- Seven components and a small reducer are cheaper to own than an adapter over an untyped catalog API.
- The engine is pure Kotlin, so its tests run on JVM and iOS from `commonTest`.
- We carry the protocol ourselves. Conformance cases from `a2ui-project` are ported (Apache-2.0,
  attribution kept in the test file) so we notice drift, and a shared golden fixture between server and
  client is a follow-up.
- If `androidx.a2ui` matures and gains multiplatform support, only `ScentSurface` and the mapper change.
- `SurfaceReducer.apply` returns a `Reduction` (state, whether the batch applied, and notes) instead of a
  `Result`, because a batch can apply while still reporting components it could not show; `setValue` returns `Result`.
- Client problems are produced as typed `VALIDATION_FAILED` errors and logged locally; sending them to the
  server is `feature/ask-surface-error-reporting`.
