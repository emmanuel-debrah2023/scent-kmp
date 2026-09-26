---
name: scent-tdd
description: Write or audit Scent KMP tests test-first — red-green-refactor unit and Compose UI tests per ADS-STE100, and flag useless tests. Use for "write tests", "TDD this", "audit tests".
keywords: [kotlin, kmp, scent, tdd, red-green-refactor, tests, ADS-STE100]
---

# Scent TDD

The standard for *how* tests are written in the Scent repo (`shared` / `composeApp` / `server`). `scent-dev-loop` Gate 4 checks work against this skill; this skill is where the rules live.

Two modes:
- **Write mode** (default) — a feature, fix or ticket needs tests. Write them first.
- **Audit mode** — the user asks to audit, clean up or review existing tests.

Only cover the layers the change actually touches. A ViewModel fix does not need a server test pass.

---

## Step 0: Orient

1. Read `docs/architecture-guidelines.md` (ADS-STE100) sections relevant to the layer: error handling (`Either<AppError, T>`), mappers, navigation, testing.
2. Find the existing fakes in `fakes/` or `commonTest`. Reuse and extend them; never copy a fake into a single test file.
3. Check test deps exist: `kotlin-test`, `kotlinx-coroutines-test`, Turbine, Compose UI test. Add only if missing, and say so.
4. For `server` tests, load and follow `db-backend-ktor` for `testApplication`, H2 and JWT setup. This skill only supplies the behaviour checklist below.

---

## Core rules

- **Behaviour, not implementation.** A test must fail when the behaviour breaks and keep passing through a behaviour-preserving refactor.
- **Mutation check.** For each test ask: if the line under test were deleted or inverted, would this fail? If not, rewrite or drop it.
- **Fakes over mocks.** MockK only when a fake is impractical. Never `verify` internal call order that isn't part of the contract. Call counters on fakes are fine when "not called" *is* the contract (e.g. invalid input never reaches the repository).
- **Exact errors.** Assert the `AppError` subtype with `assertIs<...>(result.leftOrNull())`, never just `isLeft`.
- **Naming:** backticks, `` `[unit] [scenario] [expected result]` ``.
- **Structure:** Arrange / Act / Assert, one behaviour per test, no shared mutable state.
- **Coroutines:** `runTest` with an injected `TestDispatcher`. Use cases stay dispatcher-agnostic. `Dispatchers.setMain` / `resetMain` for ViewModels.
- **Flows:** Turbine. Assert the full `UiState` sequence, not only the last value.
- **Location:** pure logic goes in `shared/src/commonTest` so it runs on JVM and iOS. `androidUnitTest` only when Android APIs are needed.
- **Coverage is a signal, not a target.** Never add a test purely to move a number — this supersedes the old "80%+ coverage" bar; a single meaningful test beats ten that only move a percentage.

### Codebase traps
- `typealias Result<T> = Either<AppError, T>` shadows `kotlin.Result`. Watch imports in tests.
- ADS-STE100 examples build `Post` without required `fragranceIds` / `createdAt`. Don't copy them verbatim.
- Fakes must implement the full repository interface (e.g. `getSimilarFragrances`).
- No test may hit real network, Supabase or the Render URL.

---

## Write mode: the cycle

For each behaviour, one at a time:

1. **RED** — write one test. Run it. Show the failure and confirm it fails for the right reason: an assertion failure, not a compile error or missing fake. A test that doesn't compile yet because the API doesn't exist is acceptable only if you then add the minimal signature and re-run to get a real assertion failure.
2. **GREEN** — minimum production code to pass. Run it.
3. **REFACTOR** — clean test and production code with everything green. Run the module suite.

For existing working code with no tests, write **characterisation tests** first. They pass immediately; say so explicitly rather than presenting them as RED. Then apply the cycle to any fix or new behaviour.

Show a short RED/GREEN log per behaviour. Commit per behaviour group: `test(<area>): <behaviour>`.

---

## Behaviour checklists by layer

Pick the ones the change touches. Work pure logic first.

**Validator** — blank → `RequiredFieldEmpty(field)`; each invalid form → its specific error; boundary values (password 7 vs 8, username 2 vs 3); valid → `Right` with the same value.

**Mappers (DTO → domain)** — each required field missing/blank → `ParseError` with `fieldName`; optional nulls → domain defaults; null list elements filtered; `toDomainList()` drops invalid entries and never throws; invalid nested items (e.g. `PostListingDto` without price) dropped, not defaulted; enum `fromString` for every alias, case-insensitive, unknown → fallback.

**Navigation (`NavigationState`, `AppNavigator`)** — push/pop; `goBack` at root returns false and keeps root; `popToRoot`; switching tabs preserves each stack; re-tapping the active tab pops to root; one test per back-press table row (pop within tab / non-Home at root → Home / Home at root → false); shared destinations stay on the current tab's stack.

**Use cases** — invalid input → validation error and repository never called; repository `Left` propagates unchanged; success returns the domain model. For roadmap auth (Google, Apple) write the contract test with `@Ignore("<ticket>")` if the repository method doesn't exist yet. Never leave red tests on main.

**Repositories** — `success != true` and `data == null` map to the right `AppError`; `SerializationException` → `ParseError`, `UnknownHostException` → `NoConnection`, timeout → `Timeout`, other → `Unknown` with `cause` kept; cache hit skips the API; auth: success persists the token, failure does not, 401 → `InvalidCredentials`/`Unauthorized`, expired → `TokenExpired`.

**ViewModels** — `Idle → Loading → Success`; `Idle → Loading → Error` plus emission on the `error` SharedFlow; invalid input emits a validation error without `Loading`; retry re-enters `Loading`.

**Server (with `db-backend-ktor`)** — register: success, duplicate email 409, weak password 400, password stored as BCrypt hash. Login: success, wrong password and unknown email both 401 with the same message. `/me`: missing, malformed, expired and wrong issuer/audience tokens all 401; valid token returns only that user. Token has `userId`, issuer, audience, ~24h expiry.

---

## Compose UI tests

Test the stateless composable. Screens are split into `XScreen(viewModel)` → `XContent(state, onEvent…)`. Pass `UiState` and lambdas directly. No real ViewModels, Koin or network.

If a screen isn't split yet, that's the RED: write the test against the `XContent` signature you want, then refactor the screen to satisfy it.

- Prefer `runComposeUiTest` in `composeApp/src/commonTest`. Use `createComposeRule()` + Robolectric in `androidUnitTest` only when Android APIs are needed.
- Find nodes by semantics: text, `contentDescription`, role. `testTag` only when nothing user-visible exists.
- Never assert colors, pixel sizes or fonts. The theme enforces design tokens.
- Accessibility is behaviour: icon-only buttons have `contentDescription`, interactive nodes have click actions, touch targets ≥ 48dp.

Standard cases for any content screen: `Loading` shows an indicator and disables submit; `Success(empty)` shows the empty state; `Success(items)` renders items; `Error` shows `BaseErrorScreen` or `InlineErrorMessage` with retry wired; each tap invokes the right callback with the right id, exactly once.

Auth screens additionally: labelled fields, `onSubmit(email, password)` receives the typed values, validation errors appear under the right field, Google/Apple buttons invoke their stubbed callbacks.

Navigation shell: four tabs, selected state in semantics, and with a real `AppNavigator` a detail opened in one tab survives switching away and back.

---

## Audit mode

Find all tests (`**/src/*Test/**/*.kt`, `server/src/test/**`). Classify each test method:

| Verdict | Meaning |
|---|---|
| **DELETE** | Asserts a fake returns what it was given; tests data-class getters, `copy` or `equals`; tests Kotlin, Koin, serialization or Ktor themselves; no assertions; unreachable-branch `isLeft` checks; pure call-order `verify`; duplicates |
| **REWRITE** | Right intent, bad form: `isLeft` without subtype; reflection into privates; mostly mocks; real delays or `Thread.sleep`; shared state; asserts on log strings or `toString()` |
| **KEEP** | Meaningful and well-formed |

Also list gaps: important behaviour with no test, prioritised by risk (auth, money, data loss first).

Output a table (`file · test · verdict · reason`) and the gap list. **Do not delete or edit anything until the user approves.** Then fix REWRITEs and fill gaps using write mode.

---

## Verify

- Run the suites for every touched module: `./gradlew :shared:allTests`, `:composeApp:testDebugUnitTest`, `:server:test`.
- Run new tests twice to catch flakiness.
- If running inside `scent-dev-loop`, hand back to Gate 5. Otherwise run `./gradlew ktlintCheck detekt allTests`.

## Report

- Tests added per layer, one line each on what they protect.
- Production code changed to satisfy tests (e.g. screens split into `XContent`).
- Bugs the tests uncovered.
- Audit mode: DELETE / REWRITE / KEEP counts and remaining gaps.
- Follow-ups worth a ticket (list them; `scent-ticket` can log them if asked).
