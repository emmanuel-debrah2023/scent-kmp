# ADR-0004: Server-driven Fragrance Detail over A2UI, streamed as JSONL

- **Status:** Proposed (becomes Accepted when this PR merges)
- **Date:** 2026-10-30
- **Deciders:** Emmanuel
- **Ticket:** Scent Tasks Tracker, "ADR-0004: server-driven Fragrance Detail with A2UI" (branch `chore/adr-0004-a2ui-fragrance-detail`)

## Context

The ask-back flow proved the A2UI path end to end (ADR-0003, `docs/ai-agents.md`). Fragrance Detail is
the second surface and the Guild demo ("one fragrance, four viewers"): the same fragrance renders
differently depending on who is looking, and the server decides the layout.

The Notion tickets for this work refer to an "ADR-0003 rules table" and a "Scent Fragrance Detail (A2UI)"
design canvas. Neither exists in the repo, its worktrees or its git history, and `design.md` has no
Fragrance Detail screen. This ADR is therefore the source of the table, and the canvas criterion on the
ticket is replaced by "approved in review, canvas not in the repo". ADR-0003 is the client engine, so
this is ADR-0004; the tickets' file name and number are stale.

## Decision

### 1. Facts and layout are decided on the server, in pure functions

Server only, in `server/.../fragrancedetail/`. The viewer's collection, reviews and listings are all
server data, and putting the rules in `:shared` would make the client fetch what the server already
knows, which defeats a server-driven screen.

- `ViewerState`, `MatchScorer`, `PriceStats`, `OwnerValue` and `SectionPlanner` take plain values and
  return plain values, so each table row below is a JVM test with no database.
- `ViewerFactsSource` is the only part that reads the database. It returns `Either<AppError, ViewerFacts>`.
  It needs a typed notes query (note plus `NoteType`), because the existing `notesFor` returns names only
  and `MatchScorer` weights by tier.
- Failures are values (`Either`), not exceptions (ADS-STE100).

### 2. Viewer state (precedence runs top to bottom)

1. No token: **SignedOut**.
2. The viewer's relation to this fragrance:
   - OWNS: **Owns**.
   - WISHLIST: **Wishlist**.
   - TRIED with the viewer's own review of 4 or more: **Liked**.
   - TRIED with a review of 2 or less: **Disliked**.
   - TRIED rated 3, TRIED with no review, or DESTASHED: no relation. The status still shows in the
     collection control.
3. With no relation, the **taste set** is the notes of the viewer's OWNS fragrances plus the notes of
   fragrances they reviewed 4 or more. An empty taste set is **ColdStart**; otherwise **Browsing(Strong)** or
   **Browsing(Weak)**.

Ratings come from `Review`, never from `CollectionEntry`.

### 3. Sections and the rules table

Each section is one component id.

| Id | Component |
|---|---|
| `header` | `FragranceHeader` |
| `collection` | `CollectionStatusControl` |
| `sign_in` | `Button` (client-local `sign_in`) |
| `match` | `MatchSummary` |
| `taste_prompt` | `Text` |
| `notes` | `NotePyramid` |
| `market` | `PriceStats` |
| `listings` | up to 3 `ListingRow`, best value first |
| `sell` | `Button` (client-local `create_listing`) |
| `no_listings` | `Text` |

| Viewer row | Ordered sections |
|---|---|
| Signed out | header, notes, market, listings, sign_in |
| Cold start | header, collection, notes, taste_prompt, market, listings |
| Strong match | header, match, collection, notes, market, listings |
| Weak match | header, collection, notes, match (with contrast), market, listings |
| Owns | header, collection, market (with owner value), sell, notes |
| Wishlist | header, collection, market ("Best value first"), listings, match, notes |
| Tried, 4 or more | header, collection, listings, market, match, notes |
| Tried, 2 or less | header, collection, match (with contrast), notes |

Tried, 2 or less has no market or listings: the app does not sell someone a scent they disliked.
Wishlist moves the market above the match because a wishlisting viewer is price-watching.

Two modifiers apply on top of any row:

- **No listings:** `market` and `listings` become `no_listings`. An owner keeps `sell`.
- **No notes:** drop `notes`, `match` and `taste_prompt`, and do not call the agent.

Signed-out and cold-start viewers get no `match`: there is no taste set to compare against.

### 4. Match strength and contrast

- Tier weights: BASE 3, MIDDLE 2, TOP 1.
- `matchedWeight` is the summed weight of this fragrance's notes that are in the taste set. `totalWeight` is
  the summed weight of all its notes.
- **Strong** when `2 * matchedWeight >= totalWeight` and at least two notes match. Otherwise **Weak**.
  Integer arithmetic leaves no float edge case at exactly half, and the two-note floor stops a one-note
  fragrance scoring 100%.
- **Contrast** is this fragrance's notes that are not in the taste set, heaviest tier first, at most three.
  It shows on a weak match and on Tried, 2 or less. The alternative, "notes shared with fragrances you
  rated 2 or less", was rejected for v1: few users have low reviews and it reads as negative.

### 5. Price facts

- `PriceStats` uses active listings only (the existing `activeListingFilter()`).
- £/ml: SEALED uses `nominalSizeMl`, OPENED and TESTER use `remainingMl`, DECANT uses `nominalSizeMl`
  (the vial size). A missing `remainingMl` is excluded from the stats, never defaulted. An empty list gives
  no stats (`null`), not zeros.
- An owner's market value is `bottleSizeMl` times the median £/ml. `CollectionEntry` has no fill level, so
  `bottleSizeMl` is the only size available.

### 6. The scent/v1 catalog grows, and facts live only in the data model

`Text.text` is a literal, so the current catalog cannot show a fact. Rather than loosening `Text`, add typed
components whose fact props are all `DataBinding`s (ADR-0003 rule kept):

| Component | Props | Binds |
|---|---|---|
| `FragranceHeader` | name, brand, concentration | `/fragrance/{name,brand,concentration}` |
| `MatchSummary` | `label`, tier, matched, contrast?, explanation? | `/match/{tier,matched,contrast,explanation}` |
| `NotePyramid` | top, middle, base | `/notes/{top,middle,base}` |
| `PriceStats` | `label`, perMl, median, bestValue, count, ownerValue? | `/market/{perMl,median,bestValue,count,ownerValue}` |
| `ListingRow` | price, size, condition, perMl, `action?` | `/listings/<i>/{id,price,size,condition,perMl}` |
| `CollectionStatusControl` | `label`, options, value, requested, `action` | `/options/status`, `/viewer/status`, `/viewer/requestedStatus` |

- Wording (`label`) is a Kotlin literal on the server; facts are never literals. Prices are formatted
  strings (`£2.10`), the same convention as the ask surface.
- `tier` is a string, not an enum (`CatalogSchemaGenerator` rejects enum kinds), and `count` is a binding
  (`CatalogSchemaTest` forbids literal numbers in props).
- An older client meets a new component as `Unknown`, then `Unsupported`, then the quiet fallback. No change
  to that path.
- Collection status is **not optimistic**. The control writes `/viewer/requestedStatus`; what it shows
  selected is the server's `/viewer/status`. A failed action leaves nothing to roll back.
- Accessibility ships with each component, in the same ticket: the name is a heading, the match tier is in
  text and not only colour, the note pyramid reads tier by tier, the price stats get a spoken form
  (`SurfaceLabels`), and a late explanation patch is a polite live region so it announces once.

### 7. Actions

Wire names stay snake_case (`answer_questions` is already on the wire). `ScentAction` gains a `local` flag
and each route rejects the other kind with 400.

| Action | Handled by | Notes |
|---|---|---|
| `set_collection_status` | server | Context `{fragranceId: /fragrance/id, status: /viewer/requestedStatus}`; status is OWNS, WISHLIST, TRIED or null (null removes the entry). Needs a JWT. Calls the collection write in process, not over HTTP. |
| `open_listing` | client | `{listingId}` |
| `open_marketplace` | client | |
| `create_listing` | client | `{fragranceId}` |
| `sign_in` | client | |
| `retry` | client | Re-opens the GET. A server round trip would add nothing. |

An action reply never writes `/` or `/match/explanation`, so it cannot overwrite the agent's late text.
The server is stateless and could not re-send it.

### 8. Transport: chunked JSONL over GET, a third repository shape

- `GET /api/v1/surfaces/fragrance/{id}`, JWT optional (`authenticate("auth-jwt", optional = true)`). It
  streams `application/jsonl`, one `A2uiJson` message per line, flushed: `createSurface`, `updateDataModel`
  (facts plus the rules explanation), `updateComponents`. The agent's text (later work) arrives as a late
  `updateDataModel` patch.
- `POST /api/v1/surfaces/fragrance/actions` (surface id in the body, mirroring the ask routes) replies with
  an ordinary JSON `A2uiMessageBatch`, never a stream and never `createSurface`. The path is deliberately
  not `/surfaces/{surfaceId}/actions`, which would also match `/surfaces/ask/actions`.
- Each GET mints a fresh surface id, `fragrance-<fragranceId>-<uuid v4>`, because v0.9 makes a duplicate
  `createSurface` an error. The action route checks the id's shape, reads the fragrance id from it,
  applies the write, reloads the viewer facts and replans. No session state.
- **The whole plan is computed before the first byte is written**, so every expected failure is a normal
  400, 404 or 503 with an `ErrorResponse`. After headers are sent, only unexpected failures remain. A2UI
  v0.9 has no server-to-client error message, so these surface on the client as a cut-off stream.
- A client disconnect makes `write` or `flush` throw, which cancels the writer coroutine and any agent
  call with it.
- Client: `SurfaceApi` streams with `prepareGet().execute` and `bodyAsChannel().readUTF8Line(max)`, with a
  bound of 256 KiB per line, and a per-request timeout (`requestTimeoutMillis` 45 s, `socketTimeoutMillis`
  20 s) because the shared 30 s timeout covers a whole stream. 45 s covers a Render cold start plus the
  agent budget.
- The repository exposes `Flow<Either<AppError, SurfaceMessage>>`. A non-2xx status is one Left then the
  flow completes; `createHttpClient` has no `expectSuccess`, so statuses are mapped by hand as
  `AskSurfaceRepositoryImpl` does. Timeout, IO and unknown exceptions map to `AppError` as a final Left and
  the flow completes; `CancellationException` is rethrown. A line that fails to decode is a `ParseError`
  Left but does **not** end the flow, so the screen decides what a bad line means (section 10).
- The ViewModel collects the repository's Flow through a Flow-returning use case
  (`ObserveFragranceSurfaceUseCase`). The guidelines' `suspend` use-case interface cannot return a Flow, so
  this is a deliberate second use-case shape, as ADR-0001 already allows for its Flow reads.

**Relationship to ADR-0001.** This is a third repository shape: neither a one-shot `suspend` returning
`Either`, nor a Room-backed single source of truth Flow. It is a network-only `Flow` that the screen
reduces into a `Surface`. There is no Room table behind it, because the surface is a view of server state
at the moment of the request. It keeps the `get...Flow` name and the `Either` boundary from ADR-0001, but it
is deliberately **not** a single source of truth: the suffix means "returns a Flow", not "Room-backed".

### 9. Authentication

A bad or expired token still gets 401 under optional auth (Ktor 3.3.3 challenges on invalid
credentials). The app only shows the main graph when signed in, so a 401 there means the session expired:
map it to `AuthError.TokenExpired` and show the fallback. Do not silently retry signed out.

The signed-out row is real server behaviour (a request with no token), but the app cannot show it today.
The Guild demo shows it with `curl` beside the phone; guest browsing is a follow-up ticket.

### 10. Screen state

`FragranceDetailUiState(phase, isSlow)` with a sealed `DetailPhase`:

- `Loading` until the `root` node exists.
- `Showing(surface, pending, actionError)`. Once the root exists, a `ParseError` line is logged and ignored
  and the surface stays; a transport Left (status, timeout, IO) ends the stream, and the screen keeps
  `Showing` if it already has a root and goes to `Fallback` if it does not.
- `Fallback(error, detail: UiState<Fragrance>)`: the native header and notes from
  `GetFragranceDetailUseCase`, with a Try again banner. The stream ending without a root is a
  `SurfaceError.Invalid` and goes here.

This uses the architecture guidelines' sealed-phase exception for multi-phase A2UI surface screens:
`set_collection_status` is a follow-up round trip that must keep the surface on screen when it fails, and
`Fallback.detail` is the plain load-then-show part, so it is a `UiState<T>` as the guidelines require.

The ViewModel keeps failures in its own state and does not use `BaseViewModel.error` (nothing collects it,
see `fix/surface-viewmodel-errors`), as `AskViewModel` already does. It follows the remaining
sealed-phase conditions in `.claude/rules/ads-viewmodel-uistate.md`: a read-only `StateFlow` over a private
`MutableStateFlow`, `handleResult` with an explicit `onError`, the reducer state private to the ViewModel,
and tests per phase transition, including a failed action that keeps the surface. The screen translates a
navigation event into a callback to the per-tab host; it never passes navigation state into leaves.

One-shot navigation from local actions (`open_listing` and so on) is a state field,
`navigation: DetailNavigation?`, with `onNavigationHandled()`, not a new Channel. It is needed from the
allowlist ticket onward.

### 11. A shared `SurfaceSession`

About 80 lines of `AskViewModel` (apply a batch, set a value, build an action, the rejected-batch result)
move into `SurfaceSession` in `shared/.../domain/surface/`. The slow-timer is its own type,
`SurfaceRoundTrip`, so the session stays pure and testable in `commonTest`. This lands as its own PR with
`AskViewModelTest` unchanged, which is the proof that Ask's behaviour did not move.

### 12. The explanation agent (later ticket)

It follows `AskBackValidator`'s trust boundary. `ExplanationValidator` rules:

- 1 to 140 characters.
- No digits, `£` or links.
- Every note from the vocabulary that the text mentions must be in matched plus contrast. Non-note
  descriptors such as "smoky" pass.
- A rejection carries only the rule's name. One retry, then the rules text.
- The model sees a projection only: note tiers, matched and contrast notes, and rating buckets. No user
  ids or names.
- The late patch has a budget (10 s default; the free model's measured median is about 10 s). Past it,
  nothing is written, because the rules text is already in the data model.

### 13. Demo seeding

A dev-only `POST /api/v1/dev/seed-viewers` (mounted behind the existing fake flags) reuses the seed
catalogue and `upsertLoginUser`, resets the demo users' rows and returns the demo fragrance id:
`viewer-cold` (empty collection), `viewer-fan` (owns two fragrances overlapping the demo one, a strong
match) and `viewer-owner` (owns it, with `bottleSizeMl`). The wishlist state is shown live by tapping
Wishlist as cold or fan.

## Alternatives considered

- **One batch response, like ask.** Less new code, but once the agent exists the whole screen waits for its
  budget. Rejected; the repository signature is a `Flow` either way, so it stays available as a fallback.
- **SSE.** New plugins and event framing for no gain over JSONL here.
- **Loosening `Text` with a binding.** Smallest change, but untyped and not accessible per fact.
- **Rules in `:shared`.** Rejected, see decision 1.
- **A debug toggle for the signed-out viewer.** Diverges from real behaviour.

## Consequences

- Koin bindings (the repository, `SurfaceSession`, `SurfaceRoundTrip`, the ViewModel, and a server
  `fragrancedetail` module with `get()` only in `module()`) belong to the PRs that add each class.
- Once this is Accepted, `docs/architecture-guidelines.md` "Repository Layer: Two Shapes" is stale; it
  needs a pointer to this third shape, and the sealed-phase exception should name `FragranceDetailViewModel`
  as its second user. That edit is left to the screen PR so the guideline changes with the code.
- The catalog grows from seven to thirteen components, with a mapper node, a binding reader, a renderer and
  an accessibility contract for each.
- ADR-0001 now has a third repository shape to point at; the screen ticket's "UiState" and "suspend +
  Either" wording is superseded by sections 8 and 10.
- A cold-start or signed-out viewer cannot yet reach the screen in the app. The temporary entry point is a
  Marketplace listing tap (`Listing.fragranceId`), tracked as `feature/listing-detail-screen`; guest
  browsing is `feature/guest-browsing`.
- Unverified until the JSONL spike: whether the iOS client (Darwin) streams as bytes arrive and honours
  the socket timeout, and whether Render's proxy buffers chunked responses.
