---
name: android-dev-loop
description: Iterative quality-gate loop for Android and Kotlin Multiplatform (KMP) code changes — keeps working a change until it compiles, follows the project's own architecture guidelines and design system, passes ktlint/detekt, and has passing unit tests. Use this whenever the user asks you to implement, fix, or refactor code in an Android/KMP repo (e.g. Scent) and cares about it being done properly, not just "make it work" — phrases like "make sure this compiles and passes lint", "write this properly with tests", "don't just hack it in", "run the full check before you call it done", or any non-trivial feature/bugfix ticket in an Android codebase. Don't wait for the user to spell out all four gates — if they're asking for real Android/KMP code changes, run this loop by default rather than stopping at "it looks right."
---

# Android Dev Loop

A change isn't done because it looks plausible — it's done when it compiles, fits how this codebase is actually built, passes static analysis, and is covered by tests that pass. This skill runs those four gates in order, every time, and loops back to earlier gates whenever a later one forces a change.

Grounded in the official Android team's guidance from [android/skills](https://github.com/android/skills) (especially `testing/testing-setup`) — read `references/android-skills-notes.md` for what was pulled from there and why.

## Step 0: Orient yourself before touching code

Don't assume conventions — find them:

1. **Architecture/design-system doc.** Look for `docs/architecture-guidelines.md`, `ARCHITECTURE.md`, `AGENTS.md`, or a `docs/` folder with similar naming. If the repo is Scent, it's `docs/architecture-guidelines.md` — read it fully before writing code, not just the section that seems relevant, since patterns (null-safety boundary, `Either` error handling, Koin DI, navigation) interact with each other.
2. **Design system location.** Find the theme/tokens (`ui/theme/`, `Theme.kt`, `Color.kt`, `Typography.kt`) and any shared reusable components (`ui/components/`). New UI should reuse these, not redeclare colors/spacing/typography inline.
3. **Module & target shape.** Is it a single Android app, or KMP with `shared`/`composeApp`/`server`-style modules? This determines which Gradle tasks apply to a given file (see `references/gradle-commands.md`).
4. **DI framework.** Koin, Hilt, or vanilla Dagger — check `libs.versions.toml` and any `di/` package. New classes get constructor injection wired into the existing module structure, not manually instantiated.
5. **Lint/static-analysis tooling.** Check for `ktlint` and `detekt` plugins in `build.gradle.kts` / `libs.versions.toml`. If neither exists, don't skip the gate — install both with sensible defaults (ktlint via the official Gradle plugin, detekt via its Gradle plugin with the default ruleset) before proceeding. Say what you set up.

## The loop

Work one logical change at a time (one ticket, one bugfix). After every edit, run gates in this order. If a gate fails, fix it and restart from Gate 1 — a fix for a lint violation can reintroduce a compile error, a new test can reveal an architecture violation, and so on. Don't skip ahead on the assumption that an earlier gate is still clean.

### Gate 1 — Compiles

Run the Gradle compile task(s) for every target the change touches (see `references/gradle-commands.md` for exact task names by module type). For KMP, a change to `commonMain` must compile for every target that consumes it, not just the one you're testing against — don't declare victory on a single-platform compile.

### Gate 2 — Architecture & design-system conformance

Reread the guideline doc found in Step 0 and check the diff against it specifically. Common things to verify (adapt to what the project's own doc actually says):

- DTOs stay nullable at the network boundary; domain models stay non-null with sensible defaults; mapping happens in one dedicated place, never scattered.
- Expected failures use the project's result type (e.g. `Either<AppError, T>`) instead of thrown exceptions, if that's the established pattern.
- New dependencies are constructor-injected and wired through the existing DI module structure, not service-located or manually `new`'d.
- New screens/components reuse the existing design-system tokens and shared components instead of hardcoding colors, spacing, or one-off text styles.
- Navigation follows whatever pattern the project has standardized on — don't introduce a second navigation approach alongside an existing one without it being the explicit point of the task.

If something doesn't fit, fix the code to match the doc — don't fix the doc to match the code, unless the user explicitly asked you to change the guideline itself.

### Gate 3 — Static analysis (ktlint + detekt)

Run `ktlintFormat` first (auto-fixes pure style so you're not hand-editing whitespace), then `ktlintCheck` and `detekt`. Fix whatever remains by hand — don't suppress rules with `@Suppress` or baseline exclusions just to get green, unless the rule is genuinely wrong for this codebase (and if so, say so to the user rather than silently suppressing it).

### Gate 4 — Unit tests

Follow the android/skills testing-setup philosophy: write or update unit tests for **business logic** — ViewModels, repositories, use cases, mappers, validators — not for Activities, Composables, or DI configuration files themselves (those get behavior/screenshot tests only if the project already has that infrastructure; don't bolt on a new testing framework for one ticket without checking Step 0 first).

- Prefer fakes over mocks for repositories/dependencies where a fake is feasible; reach for a mocking library only when a fake isn't practical.
- Match the project's existing test conventions (Arrange-Act-Assert structure, existing Flow-testing library, existing mocking library) rather than introducing new ones.
- Run the test task for every module you touched or whose behavior could be affected. A green run on the module you edited doesn't mean a downstream module still compiles/passes.

## Stopping conditions

Keep looping until all four gates are clean. If you hit the same failure twice in a row without a clear fix, or you're past a reasonable number of iterations (a good rule of thumb: 5), stop and surface to the user exactly what's still failing and what you tried — don't keep silently retrying the same fix, and don't quietly ship a partial pass (e.g. "tests pass but detekt still has 3 violations") without flagging it explicitly.

## Reporting back

When the loop finishes, tell the user what changed at each gate that mattered — not a blow-by-blow of every iteration, just: what was implemented, what the architecture/design-system check caught (if anything), what lint/detekt caught (if anything), and what tests were added/updated. If Step 0 required bootstrapping ktlint/detekt because they didn't exist, call that out explicitly since it's a repo-wide change, not just something scoped to this one ticket.
