---
paths:
  - "**/commonMain/**"
  - "**/commonTest/**"
---

# commonMain purity

Code here compiles for Android **and** iOS. A `java.*` reference compiles
cleanly on Android and fails the Kotlin/Native build — so it looks correct in
every fast feedback loop you have and breaks at the slowest one. A write-time
hook blocks the common cases; this covers the substitutes and the judgment.

## Never in commonMain

`java.*`, `javax.*`, `android.*`, `kotlinx.coroutines.Dispatchers.IO`,
`System.*`, `Thread.*`.

## Substitutes

| Instead of | Use |
|---|---|
| `java.time.*`, `SimpleDateFormat` | `kotlinx-datetime` (`Instant`, `Clock.System.now()`, `LocalDateTime`) |
| `System.currentTimeMillis()` | `Clock.System.now().toEpochMilliseconds()` |
| `java.util.UUID.randomUUID()` | `kotlin.uuid.Uuid.random()`, or an `expect fun` if on an older Kotlin |
| `java.util.Random` | `kotlin.random.Random` |
| `String.format(...)` | string templates, or `padStart` / `toString(radix)` |
| `java.io.File` | `okio` (`Path`, `FileSystem`), or push file access to platform code |
| `Dispatchers.IO` | `Dispatchers.Default` — `IO` is JVM-only |
| `Thread.sleep(n)` | `delay(n)` inside a coroutine |
| `java.util.concurrent.*` | coroutines, `Mutex`, `atomicfu` |
| `Charset`, `getBytes()` | `encodeToByteArray()` / `decodeToString()` |
| `Locale`-dependent case ops | `lowercase()` / `uppercase()` with no argument |
| `java.util.regex` | `kotlin.text.Regex` |

## When there is no common API

Use `expect`/`actual`, and complete **both** sides in the same change — an
`expect` with only an Android `actual` is a broken iOS build that hasn't
surfaced yet. `TokenStorage` in `docs/auth.md` is the reference example:
interface in `commonMain`, DataStore on Android, Keychain on iOS.

Prefer pushing the platform dependency behind an interface in `commonMain` over
sprinkling `expect`/`actual` through business logic.

## Verifying

An Android-only compile proves nothing. When you change `commonMain`, build an
iOS target too:

```
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Where java.* is fine

`androidMain`, `jvmMain`, and the whole `server` module. The Ktor backend runs
on the JVM — `java.time.LocalDateTime` in `AuthRoutes.kt` is correct and should
stay. The constraint is about `commonMain` specifically, not about the codebase.
