---
name: scent-dev-loop
description: |
  Iterative quality-gate loop for Scent KMP changes — ensures changes compile,
  follow ADS-STE100 architecture guidelines, pass ktlint/detekt, have unit tests,
  and clear pre-push verification. Use this whenever implementing features,
  bugfixes, or refactors in the Scent repo and the work needs to be production-ready.
  Scent-specific variant of android-dev-loop with all five gates enforced.
keywords: [kotlin, kmp, scent, quality-gate, architecture, lint, tests, ADS-STE100]
---

# Scent Dev Loop

A change isn't done because it compiles or looks plausible — it's done when it compiles across all targets, follows ADS-STE100 (the Scent codebase contract), passes static analysis, is covered by tests, and clears pre-push verification.

This skill runs five mandatory gates in order. If a gate fails, fix it and restart from Gate 1 — a fix for a lint violation can reintroduce a compile error, a new test can reveal an architecture violation, and so on.

**CRITICAL RESOURCE:** All code changes must comply with **ADS-STE100** (`docs/architecture-guidelines.md`), the authoritative design doc for Scent. This is not optional guidance — it's the contract for this codebase.

**TESTING STANDARD:** How tests are written — test-first cycle, fakes, assertions, UI tests, what counts as a useless test — lives in the **`scent-tdd`** skill. This loop does not restate those rules; it enforces them.

---

## Step 0: Orient — Read ADS-STE100 First

Before touching any code:

1. **Read `docs/architecture-guidelines.md` (ADS-STE100) fully** — not just the section that seems relevant. This doc covers:
    - **Null-safety strategy**: DTOs are nullable; domain models are non-null with sensible defaults; mapping happens in dedicated places.
    - **Error handling**: All failures use `Either<AppError, T>` (never thrown exceptions for expected failures).
    - **Dependency Injection**: Constructor injection wired through Koin modules, never service-located.
    - **Repository pattern**: All repository methods return `Either<AppError, T>`; no exceptions for expected failures.
    - **ViewModel design**: Use `UiState<T>` sealed class for state; expose errors via `SharedFlow`.
    - **Navigation**: Bottom-nav with four tabs, each owning an isolated back stack (per-tab sealed routes + `NavigationState<Route>`, tied together by `AppNavigator`); designed for migration to official Compose Navigation nested graphs later.
    - **Testing**: Prefer fakes over mocks; cover business logic and stateless composables, not framework code.
    - **Form validation**: Use validators returning `Either<AppError, T>`.

   These patterns interact — null-safety affects mapping, mapping affects error handling. Skipping this step will cause rework later.

2. **Locate design system tokens** — Find `ui/theme/`, `Theme.kt`, `Color.kt`, `Typography.kt`. New UI must reuse these, not hardcode colors/spacing/typography.

3. **Understand module shape** — Scent is KMP with `shared` / `composeApp` / `server` modules. Know which Gradle tasks apply to your files.

4. **Check DI wiring** — Scent uses Koin. Verify `networkModule`, `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule` exist and that new classes are constructor-injected into the right module.

5. **Write the failing tests first** — For any new behaviour or bugfix, load `scent-tdd` and write the RED tests before the implementation. The gates below run after the code exists; the tests should already be there by then. For a bugfix, the first test reproduces the bug.

---

## Gate 1: Compiles

Run Gradle compile for every target you touched:

```bash
# For changes to shared module (commonMain)
./gradlew :shared:compileKotlin

# For Android-specific code
./gradlew :composeApp:compileDebugKotlin

# For iOS-specific code
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# For all targets (comprehensive check)
./gradlew assemble
```

**KMP caveat**: A change to `commonMain` must compile for every target that consumes it — don't declare victory on a single-platform compile.

---

## Gate 2: Architecture & Design-System Conformance (ADS-STE100)

**MANDATORY**: Reread ADS-STE100 (`docs/architecture-guidelines.md`) and check the entire diff against it. This is not a suggestion — every merge must pass this gate.

### Null-Safety & Serialization
- ✅ DTOs (in `data/dto/`) stay nullable for all fields — they handle unreliable server responses.
- ✅ Domain models (in `domain/model/`) are non-null with sensible defaults (empty lists, zero counts, etc.).
- ✅ Mapping happens in dedicated mapper files (e.g., `data/mapper/FragranceMapper.kt`), never scattered across repos or ViewModels.
- ✅ Mappers return `Either<AppError, DomainModel>` — never just map blindly.
- ✅ `FilterNotNull()` is used on nullable lists; invalid entries are dropped, not crashing.

### Error Handling
- ✅ All repository methods return `Either<AppError, T>` — no exceptions for expected failures.
- ✅ `Either` is standard; use `asRight()` / `asLeft()` helpers.
- ✅ `AppError` is sealed with subclasses for each category (NetworkError, ValidationError, AuthError, etc.).
- ✅ ViewModels expose errors via `SharedFlow<AppError>` for UI snackbars/toasts.
- ✅ UI errors use provided components (`ErrorScreen`, `InlineErrorMessage`, `ErrorSnackbarHost`).

### Dependency Injection
- ✅ New dependencies are constructor-injected, never service-located or manually `new`'d.
- ✅ All classes wired through Koin modules (`networkModule`, `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`).
- ✅ No `get<T>()` calls inside class bodies — that's a code smell.

### ViewModel Design
- ✅ ViewModels expose `StateFlow<UiState<T>>` for UI state (Idle, Loading, Success, Error).
- ✅ Use `UiState` sealed class consistently across all ViewModels.
- ✅ Business logic lives in use cases or repositories, not ViewModels.
- ✅ ViewModels delegate errors to `SharedFlow` and use helper methods like `handleError()`.

### Navigation
- ✅ Routes belong to their tab's sealed hierarchy (`HomeRoute`, `MarketplaceRoute`, `SearchRoute`, `ProfileRoute`); each tab has its own `NavigationState<Route>` inside `AppNavigator`.
- ✅ Shared destinations (`FragranceDetail`, `ListingDetail`, `UserProfile`) stay duplicated per tab by design — do not extract a shared `DetailRoute`.
- ✅ Navigate to the current tab's own copy of a shared destination so the entry stays on that tab's stack.
- ✅ Back press goes through `AppNavigator.onBackPressed()` — no ad-hoc back handling.
- ✅ Pass navigation callbacks down (e.g., `onNavigateToDetail: (String) -> Unit`); don't inject `NavigationState` into deep components.
- ✅ No ad-hoc navigation libraries or two concurrent approaches without explicit user request.

### Testing
- ✅ Tests follow `scent-tdd`: written first, behaviour-focused, fakes over mocks, exact `AppError` subtypes asserted.
- ✅ Every `Either.Left` path introduced by the change has a test.
- ✅ Screens are split into `XScreen(viewModel)` → stateless `XContent(state, callbacks)` so the content is UI-testable.
- ✅ No useless tests added (see `scent-tdd` audit criteria): no testing fakes, getters, or framework code.

### Design System
- ✅ New screens/components reuse tokens from `ui/theme/` (colors, spacing, typography).
- ✅ No hardcoded color hex values or dimension literals.
- ✅ Use `MaterialTheme.colorScheme`, `MaterialTheme.typography`, etc.
- ✅ Reuse shared components from `ui/components/`.

### Independent second opinion

After your own pass above, spawn the **`ads-ste100-auditor`** subagent (`.claude/agents/ads-ste100-auditor.md`). Self-review is biased toward the code you just wrote; the auditor starts with fresh context, reads the full guideline and the changed files itself, and is read-only, so it can only report.

- Give it the diff range or file list and nothing else. Don't pass your own conclusions, justifications or "already checked" notes — they anchor it and defeat the point.
- A `FAIL` verdict or any blocking violation fails Gate 2, even if your self-check passed. Fix in the main thread, then re-run the auditor.
- Treat `GUIDELINE GAPS` as a prompt to tell the user, not to edit the guideline.
- Record the outcome with `scripts/gates.sh --gate2 pass|fail "<auditor verdict>"`.
- For a small single-file change, `references/gate2-checklist.md` alone is enough; skip the subagent.

**Action**: If the code doesn't match ADS-STE100, fix it — don't rewrite the guideline. If you believe the guideline needs updating, surface that explicitly (with explanation) rather than silently violating it.

---

## Gate 3: Static Analysis (ktlint + detekt)

```bash
./gradlew ktlintFormat ktlintCheck detekt
```

- Run `ktlintFormat` first (auto-fixes pure style).
- Then run `ktlintCheck` and `detekt`. Fix violations by hand.
- Don't suppress rules with `@Suppress` or baseline exclusions just to get green — if a rule is wrong for this codebase, say so to the user rather than silently suppressing it.

---

## Gate 4: Tests

Confirm the tests written in Step 0 per `scent-tdd` exist, cover the change, and pass. Covered layers: validators, mappers, navigation state, use cases, repositories, ViewModels, stateless composables (`XContent`), and server routes where touched. Not covered: Activities, DI module files, framework code.

```bash
# For the module you touched
./gradlew :shared:jvmTest
./gradlew :composeApp:testDebugUnitTest
./gradlew :server:test

# For all modules (comprehensive check during iteration)
./gradlew allTests
```

- ✅ Every new behaviour had a RED test before its implementation (or a characterisation test, flagged as such, for pre-existing code).
- ✅ Run tests for every module you touched or whose behaviour could be affected.
- ✅ A green run on the module you edited doesn't mean downstream modules still pass.
- ✅ New tests run twice without flaking.

If tests are missing or weak, go back to `scent-tdd` — don't write quick post-hoc tests just to pass this gate.

---

## Gate 5: Pre-Push Local Verification

Before committing or pushing, run the full local quality gate in one shot:

```bash
./gradlew ktlintCheck detekt allTests
```

**All three must be green.** If any fails:
1. Fix the failure
2. Restart from Gate 1 (a fix can reintroduce a compile error)
3. Re-run the full Gate 5 command before attempting to push

**Do not push until this command exits with BUILD SUCCESSFUL.** This is the same bar CI enforces.

### Performance note
If `allTests` is slow during iteration on a specific module, use:
```bash
./gradlew :shared:jvmTest :composeApp:testDebugUnitTest
```

But always run the full `allTests` as the final gate before push.

---

## Stopping Conditions

Keep looping until all five gates are clean. If you hit the same failure twice in a row without a clear fix, or you're past 5 iterations, stop and surface exactly what's still failing and what was tried — don't keep silently retrying the same fix, and don't ship a partial pass without flagging it explicitly.

---

## Reporting Back

When the loop finishes, tell the user:

- **What was implemented** — the feature, fix, or refactor
- **ADS-STE100 compliance** — did the change pass Gate 2? Call out any architecture issues caught and fixes applied (e.g., "Converted from exception-based error handling to `Either<AppError, T>`", "Added mappers for nullable DTOs", "Wired new repository through Koin").
- **Static analysis** — what lint/detekt violations were fixed
- **Tests** — what tests were written first and what each protects; any bugs they caught
- **Gate 5 result** — confirmed all three checks pass locally before push

**Always restate**: The change is ready to push only after Gate 5 passes clean.

---

## Scent-Specific Reminders

- **Auth flows**: Follow Phase 1/2/3 patterns from `docs/auth.md` — JWT is current; Google OAuth is next; Apple Sign-In is required for iOS App Store.
- **Fragrance data**: Use the `Fragrance` domain model (non-null, with defaults) — never expose nullable `FragranceDto` to UI.
- **Error display**: Use the provided error components for consistency; don't roll one-off error handling.
- **Marketplace features**: Listings and commerce logic must use `Either<AppError, T>` for transaction-safety patterns.
- **KMP specifics**: Changes to `shared/src/commonMain/` must compile on all targets; `androidMain` and `iosMain` are platform-specific.
- **Server tests**: Use `db-backend-ktor` for `testApplication`, H2 and JWT test setup.
