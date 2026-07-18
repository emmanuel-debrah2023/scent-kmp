---
name: scent-dev-loop
description: |
  Iterative quality-gate loop for Scent KMP changes.
  Ensures changes compile, follow architecture guidelines,
  pass ktlint/detekt, have unit tests, and clear pre-push verification.
trigger: |
  When implementing features, bugfixes, or refactors in the Scent repo
keywords: [android, kotlin, kmp, quality-gate, lint, tests]
---

# Scent Dev Loop

Before considering any change "done":

## Orient: Read Architecture First
- Read `/docs/architecture-guidelines.md` fully
- Understand: null-safety boundary, Either error handling, Koin DI, navigation
- Check: design-system tokens location, module shape (KMP targets)

## Gate 1: Compiles
\`\`\`bash
./gradlew assemble  # All targets for files you touched
\`\`\`

## Gate 2: Architecture Conformance
- DTOs: nullable at network, non-null in domain
- Errors: use Either<AppError, T>, not exceptions
- DI: constructor injection via Koin module
- UI: reuse design tokens, don't hardcode colors
- Navigation: follow existing pattern

## Gate 3: Lint & Static Analysis
\`\`\`bash
./gradlew ktlintFormat ktlintCheck detekt
\`\`\`

## Gate 4: Unit Tests
- Test business logic (ViewModels, repos, use cases, mappers)
- Skip UI layer unless behavior/screenshot tests already exist
- Prefer fakes over mocks
- Match project's test conventions

## Gate 5: Pre-Push Verification
\`\`\`bash
./gradlew ktlintCheck detekt allTests
\`\`\`

Must exit with BUILD SUCCESSFUL. Do not push otherwise.
