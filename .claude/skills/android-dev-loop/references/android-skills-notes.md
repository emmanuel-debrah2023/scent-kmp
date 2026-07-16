# Notes from github.com/android/skills

The official Android team publishes AI-optimized `SKILL.md` files at
[android/skills](https://github.com/android/skills), covering areas where they've
found LLMs specifically underperform (not general Compose/Kotlin basics). This
loop draws on a few of them directly:

## testing/testing-setup

The main source for Gate 4. Key points this skill's testing guidance is built on:

- **Analyze before installing.** Check `libs.versions.toml` and build files for
  the existing DI framework, unit test framework, and mocking framework before
  assuming or adding anything. Respect the current stack.
- **Unit test business logic only.** "Create a task to add or review unit tests
  in every file that contains business logic (ViewModels, Repositories,
  database-related classes such as DAOs, etc.). Don't create unit tests for
  Activities, Compose layouts, or dependency injection configuration files."
- **Fakes over mocks.** Prefer a fake implementation behind an interface; only
  mock when you can't create a fake (e.g. no access to the class/interface).
- **Don't install what wasn't asked for.** Only add a mocking framework, Robolectric,
  screenshot testing, or end-to-end testing if the project already uses it or the
  user explicitly asked for it.

Full skill: https://raw.githubusercontent.com/android/skills/main/testing/testing-setup/SKILL.md

## jetpack-compose/theming/styles

Relevant to Gate 2's design-system check even though its main subject (the
experimental Compose Styles API) usually won't apply. The transferable idea:
locate the central theme file and design tokens first, confirm what design-system
primitives already exist, and migrate/build components to reuse those tokens
rather than hardcoding values. Its own validation step is a good model for Gate 1/3:
"Build the project. Verify no compilation errors. Run screenshot tests. Compare
visual outputs before/after."

Full skill: https://raw.githubusercontent.com/android/skills/main/jetpack-compose/theming/styles/SKILL.md

## navigation/navigation-3

Not used directly in this loop, but worth reading if a ticket is specifically about
a navigation migration (e.g. Scent's own documented move from custom state-based
navigation to official Navigation-Compose-Multiplatform).

Full skill: https://raw.githubusercontent.com/android/skills/main/navigation/navigation-3/SKILL.md

## What wasn't there

There's no official android/skills entry for ktlint/detekt setup or a general
"build+lint+test loop" — that's this skill. Gates 1 and 3 are standard Gradle/JVM
tooling, not Android-specific skills, so they're covered here directly rather than
by reference.
