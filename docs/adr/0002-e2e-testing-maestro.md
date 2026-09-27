# ADR-0002: Adopt Maestro for Android E2E testing

- **Status:** Accepted
- **Date:** 2026-09-27
- **Deciders:** Emmanuel
- **Ticket:** Scent Tasks Tracker — "ADR: adopt Maestro for Android E2E testing" (https://app.notion.com/p/3dd11d6b186b812c9359c5ce1377417a)

## Context

Scent had unit tests (`commonTest`) and Compose UI tests, but nothing that drove
the app itself the way a user does — launching it, moving across screens, and
hitting a real backend. `chore/introduce-maestro` proved a black-box harness
(Maestro) can launch the app, wait out the splash gate, and assert the login
screen renders, with no Gradle dependency. This ADR settles the boundary that
proof was operating inside: what each test layer owns, how much of a flow runs
against real UI versus seeded state, and how flows are written so they don't
rot into flaky noise.

Two things forced this to be a decision rather than a convention doc entry.
First, a separate, still-unstarted spike ("Spike: Jev + touchpress e2e smoke
test for auth flow") targets the same territory — an agent-driven Android
harness for the auth flow — and its output is an unverified go/no-go. Without
settling how it relates to Maestro, both tracks could end up building
competing auth E2E coverage. Second, the next ticket in this epic
("Agent-driven E2E loop: Maestro MCP and scent-e2e skill") hands flow-writing
to an agent looping on write → run → read-failure → fix; that loop needs a
selector and scope policy fixed *before* the agent starts writing flows, or the
agent has nothing to fall back on when a selector choice is ambiguous.

## Decision

**Layer ownership.** Three layers, each owning a different kind of correctness:

- **Unit tests** (`commonTest`, `androidUnitTest`) — logic: mappers, use cases,
  ViewModel state transitions, repository behaviour against fakes. No UI, no
  process, fastest feedback.
- **Compose UI tests** — component rendering and interaction in isolation: does
  this composable show the right state for a given input, does tapping it fire
  the right callback. No navigation across screens, no real backend.
- **Maestro** — cross-screen user journeys against a real, running backend:
  does registering, then logging in, then landing on the feed actually work
  end-to-end, on a real (emulated) device. Maestro does not duplicate what unit
  or Compose UI tests already cover; it exists for the seams between screens
  and the real network/auth boundary that those layers stub out.

**Black-box / gray-box split.** Exactly one black-box journey — through the
real UI, no shortcuts — covers the single critical path: auth (register →
login → land on an authenticated screen → sign out). Every other Maestro flow
is gray-box:

- App state is seeded through dev-only routes (e.g. `/api/v1/dev/seed-feed`)
  rather than driven into existence by UI actions.
- The session and the start screen are injected via debug-only launch
  arguments, so a flow can open directly on, say, the Marketplace with a
  logged-in session instead of replaying register/login first.

This is a speed and determinism trade, not a coverage cut: replaying the full
auth UI journey for every flow multiplies runtime linearly with flow count and
gives every non-auth flow a second, unrelated way to flake (an unrelated auth
UI regression breaking a Marketplace flow). One black-box journey proves the
real login path works; gray-box start points let every other flow assume that
and test only what it's actually about.

**Selector and guardrail policy** (this codifies the practice already used in
`chore/introduce-maestro`'s `smoke-launch.yaml`, generalized for flows after
it):

1. **Text or accessibility-label selectors first.** Assert and tap on visible
   text or `contentDescription` — what a user actually sees — before reaching
   for anything else. This is also why `LoginScreen`'s current copy
   (`"Welcome back"`, `"Sign in"`) was usable as-is with zero production code
   changes for the first smoke flow.
2. **`testTag`/resource-id is a justified exception**, used only when no
   stable, unique text or label exists (e.g. two visually identical icon
   buttons) — and the flow file notes *why* inline as a comment when it reaches
   for one.
3. **An assertion after every action.** No `tapOn` → `tapOn` → `tapOn` chains
   that only assert at the end; each step confirms the app reached the state
   the next step assumes, so a failure points at the action that actually
   broke instead of forcing a bisect.
4. **A soak gate before a flow joins the suite.** A new flow must pass
   multiple consecutive local runs before it's treated as reviewable/mergeable
   coverage, not merely "passed once." (The mechanics — `scripts/e2e-soak.sh`
   or equivalent — land with the next ticket; this ADR fixes the requirement,
   not the script.)

**Jev/touchpress relationship.** The touchpress+Jev spike is a **complement**,
not a competing harness, and is explicitly not adopted by this ADR. Jev picks
moves from judged, probabilistic assertions over an accessibility-tree
snapshot (`toBeJudged`) — useful for screen states that are hard to pin to an
exact selector or exact text (e.g. "does this error state look right"). Maestro
owns deterministic, selector-based user journeys, including the one black-box
auth path. The spike's own acceptance criteria already gate its adoption on a
go/no-go verdict; until that lands, Scent has exactly one E2E harness building
auth coverage — Maestro — and the spike, if it goes green, is scoped to judged
assertions Maestro's selector model can't express, not to auth journeys
Maestro already owns.

**Scope: Android-first, CI and iOS deferred.** Maestro flows run locally
against the Android emulator only, no CI wiring in this ADR. Both are
deliberate deferrals, not oversights:

- **CI deferral** reverses once flows are stable enough to soak-gate (see
  policy point 4) and an emulator-capable CI runner is budgeted — running an
  unstable flow in CI before then just adds a flaky-red gate nobody trusts. A
  separate spike ("Spike: run Jev e2e suite in GitHub Actions") is already
  exploring the emulator-in-CI mechanics for the Jev track; Maestro CI wiring
  should reuse whatever that spike settles, not stand up a second CI emulator
  path.
- **iOS deferral** reverses once Scent has an iOS build worth E2E-testing on a
  simulator; today iOS is behind Android in the KMP rollout, and Maestro's iOS
  simulator support is real but unexercised here. Standing up iOS E2E now
  would test a moving target.

**Dependency footprint.** Maestro is a standalone CLI (installed via
`curl -Ls https://get.maestro.mobile.dev | bash`), not a Gradle dependency —
it never touches `composeApp/build.gradle.kts`, `libs.versions.toml`, or
`.claude/.approved-deps`, and the dependency-gate hook has nothing to check
here. This is why `chore/introduce-maestro` could land with zero Gradle diff.

## Consequences

**Good**
- A clear three-layer ownership model means a future "should this be a unit
  test, a Compose UI test, or a Maestro flow?" question has a fixed answer
  instead of being re-litigated per PR.
- The black-box/gray-box split keeps flow runtime and flake surface bounded as
  flow count grows — most flows never re-run the auth UI.
- Fixing the selector/guardrail policy now gives the agent-driven loop in the
  next ticket a concrete fallback instead of ambiguous judgment calls.
- Settling the Jev relationship means the next ticket's scope (agent writes
  Maestro flows) isn't accidentally competing with an unrelated, unverified
  spike track.

**Bad**
- The gray-box seeding mechanism (dev-route state seeding, debug-only launch
  arguments for injecting session/start screen) is *decided* here but not yet
  *built* — `chore/introduce-maestro`'s one flow needed neither, since it
  exercises the pre-auth splash/login screens directly. Building it is
  in-scope for whichever ticket writes the first gray-box flow, not this ADR.
- Soak-gating (policy point 4) has no script yet (`scripts/e2e-soak.sh` doesn't
  exist); until the next ticket lands it, "soak-gated" is a stated requirement
  without an enforcement mechanism.
- Two E2E-adjacent tracks (Maestro, Jev/touchpress) now coexist in the
  tracker even though only one is adopted — future contributors need to read
  this ADR to know Jev isn't a second E2E harness, or the distinction erodes.

**Neutral**
- Unit and Compose UI test conventions are unaffected — this ADR only adds a
  third layer above them, it doesn't change what they're responsible for.

## Alternatives considered

**Espresso / XCUITest** — rejected as the primary harness because both are
per-platform: Espresso flows don't run against an eventual iOS build, and
XCUITest flows don't run against Android today. Maestro's YAML flows are
platform-agnostic, which matters for a KMP app that intends to grow an iOS
target, even though iOS runs are deferred for now.

**Appium** — rejected for setup and maintenance cost disproportionate to
Scent's current test surface: a full Appium server/driver stack is more
infrastructure than one team maintaining a single critical-path black-box
journey plus a handful of gray-box flows needs. Maestro's CLI-only,
no-server model matches the actual scale of what's being tested.

**Jev/touchpress as the primary E2E harness** — rejected for now, not
permanently: its own spike ticket states the output is a go/no-go, not a
production suite, and its Android path is explicitly unverified (the linked
Jev PR is iOS-verified only). Adopting an unverified, judged-assertion harness
as the *primary* mechanism — ahead of a deterministic, selector-based one —
would leave Scent without any dependable E2E coverage while the spike is
still open.

## Revisit if

- The touchpress+Jev spike returns a "go" verdict with real Android coverage —
  at that point, define concretely which judged-assertion cases it takes over
  from Maestro (if any), rather than letting the two harnesses drift into
  overlapping coverage.
- Flows are stable enough to soak-gate and an emulator-capable CI runner is
  available — CI wiring for Maestro should follow immediately, reusing
  whatever the GitHub Actions Jev-suite spike settles for running an Android
  emulator in CI.
- Scent starts shipping an iOS build worth testing — iOS simulator runs move
  from deferred to in-scope.
