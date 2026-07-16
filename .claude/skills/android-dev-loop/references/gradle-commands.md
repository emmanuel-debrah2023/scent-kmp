# Gradle command reference

Adapt module names to the actual `settings.gradle.kts` — the examples below use
Scent's module layout (`shared`, `composeApp`, `server`) as a concrete case of a
typical KMP project (shared/business-logic module + Compose Multiplatform UI
module + JVM server module). A plain single-module Android app collapses most
of this to just the `app` module's Android variants.

## Gate 1 — Compile

KMP compiles per source set/target — a `commonMain` change must compile everywhere
it's consumed:

```
./gradlew :shared:compileKotlinJvm
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosArm64      # or iosSimulatorArm64 depending on host
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:compileKotlinIosArm64
./gradlew :server:compileKotlin
```

Or, faster during iteration, just compile everything and let Gradle figure out
what's affected:

```
./gradlew compileKotlin compileTestKotlin
```

Full `build` (compiles + tests + packaging) is the final confidence check, not
the per-iteration one — it's slower:

```
./gradlew build
```

## Gate 3 — ktlint + detekt

If not already present, add to the root `build.gradle.kts` (or via a convention
plugin if the project uses one):

```kotlin
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "<latest>"
    id("io.gitlab.arturbosch.detekt") version "<latest>"
}
```

Then:

```
./gradlew ktlintFormat   # auto-fix style first
./gradlew ktlintCheck    # verify, fails the build on remaining violations
./gradlew detekt         # deeper static analysis (complexity, code smells, etc.)
```

Detekt needs a config (`config/detekt/detekt.yaml`) — generate a default with
`./gradlew detektGenerateConfig` if one doesn't exist yet, then tune from there
rather than accepting every default rule blindly (some are opinionated and
worth turning off deliberately, not by accident).

## Gate 4 — Tests

KMP test tasks run per-target too. For logic in `commonMain`/`commonTest`
(where most business-logic tests live, per Gate 4's guidance):

```
./gradlew :shared:allTests          # runs commonTest against every target
./gradlew :shared:jvmTest           # just the JVM target — fastest inner loop
./gradlew :composeApp:allTests
./gradlew :server:test
```

`allTests` is the correct one to run before calling a change done — a test that
only runs against `jvmTest` can hide a target-specific `expect`/`actual` bug.

## Useful one-liners while iterating

```
./gradlew compileKotlin compileTestKotlin ktlintCheck detekt :shared:jvmTest --continue
```

`--continue` surfaces every failing task in one run instead of stopping at the
first, which is usually what you want mid-loop (fix everything you can see, then
re-run) rather than end-to-end (where you want a real fail-fast signal).
