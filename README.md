# Scent

**Early preview** — active development, not yet production-ready.

A fragrance social-commerce app built with Kotlin Multiplatform. Discover, review, and trade fragrances — all in one place.

## Screenshots

_Coming soon_

## Features

- **Auth** — email/password registration and login with JWT sessions; Google Sign-In support
- **Community feed** — text and video posts, hashtags, likes
- **Fragrance catalogue** — browse and search fragrances with notes, concentration, and condition details
- **Marketplace** — list and discover fragrance listings
- **Media** — short-form video upload and playback via Cloudflare Stream
- **Profile** — user profile with avatar, bio, and logout

## Tech stack

| Layer | Technology |
|-------|-----------|
| Mobile (Android & iOS) | Kotlin Multiplatform · Compose Multiplatform |
| Backend | Ktor · Exposed · PostgreSQL |
| Dependency injection | Koin |
| Networking | Ktor client · kotlinx.serialization |
| Video | Cloudflare Stream · ExoPlayer (Android) |
| Auth | JWT · Google OAuth |

## Project structure

```
├── composeApp/   # Shared Compose UI (Android + iOS targets)
├── shared/       # Domain models, repositories, use cases, DI
├── server/       # Ktor REST API
└── iosApp/       # iOS entry point (Xcode)
```

## Getting started

### Prerequisites

- Android Studio Meerkat or later
- Xcode 16+ (iOS builds)
- JDK 17+
- PostgreSQL instance

### Android

```bash
./gradlew :composeApp:assembleDebug
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

### Server

Copy `.env.example` to `.env` and fill in your database and Cloudflare credentials, then:

```bash
./gradlew :server:run
```

To seed the local feed with sample posts (requires `STREAM_PROVIDER=fake`):

```bash
curl -X POST "http://localhost:8080/api/v1/dev/seed-feed?count=10"
```

### HotSwan hot reload (Compose UI iteration)

Compose HotSwan gives AI-driven hot reload on a running device — structural
Compose changes land in under a second, no app restart.

- **Gradle plugin** — `2.0.0-beta04`, declared in `libs.versions.toml` as
  `hotswan-compiler`. Applied in `composeApp/build.gradle.kts` **only when the
  `hotswanEnabled` project property is set** — the plugin instruments every
  function it can reach (including `shared`'s classes) with a runtime
  interpreter shim that requires a live device/JVMTI agent to bootstrap, so
  applying it unconditionally breaks `allTests` on a bare JVM. In the HotSwan
  IDE plugin's settings, add an environment variable
  `ORG_GRADLE_PROJECT_hotswanEnabled=true` for its build/install invocations —
  Gradle maps `ORG_GRADLE_PROJECT_<name>` env vars to project properties
  automatically, so the plugin activates for HotSwan's own builds but stays off
  for `./gradlew ktlintCheck detekt allTests`.
- **IDE plugin** — install the matching `2.0.0-beta04` build from the beta
  channel at [hotswan.dev/install](https://hotswan.dev/install) (not in the
  default Marketplace search). Gradle and IDE plugin versions must match.
- **MCP config** — in Android Studio, go to
  **Settings → Tools → Compose HotSwan → Copy MCP Config to Clipboard**, then
  paste it into `.mcp.json` at the repo root. This file is gitignored — it
  embeds an absolute path to your local plugin install, so each contributor
  generates their own. Set the **App Module** to `composeApp` (not the default
  `:app`) and add the `ORG_GRADLE_PROJECT_hotswanEnabled=true` environment
  variable in the same settings panel.
- **Starting a session** — run the app on a device/emulator, then in Claude
  Code call `hotswan_get_status` to confirm connection and `hotswan_start` to
  begin watching. See the HotSwan workflow section in `.claude/CLAUDE.md` for
  the edit → reload → screenshot loop.

### Maestro E2E smoke tests

Maestro drives the app end-to-end on a real emulator/device, black-box —
no Gradle dependency, just a CLI you install once.

- **Install** — `curl -Ls "https://get.maestro.mobile.dev" | bash` (see
  [maestro.mobile.dev](https://maestro.mobile.dev) for other platforms).
  Requires `adb` on your `PATH` and a running/connected Android
  emulator or device.
- **Layout** — `.maestro/config.yaml` points at the flow files under
  `.maestro/flows/`. `smoke-launch.yaml` launches the app fresh
  (`clearState: true`), waits for the splash gate to settle, and asserts the
  login screen (`Welcome back` / `Sign in`) is visible.
- **Running a single flow** — build/install the debug APK, then run:
  ```bash
  maestro test .maestro/flows/smoke-launch.yaml
  ```
- **One-command run** — `./scripts/e2e-local.sh [flow]` lints every flow,
  installs the debug APK via `./gradlew :composeApp:installDebug`, and runs the
  flow (the smoke flow by default). Along the way it zeroes the emulator's
  window/transition/animator animation scales (restored on exit, even on
  failure) so Maestro isn't waiting out animation timing.
- **Rules and gates** — `./scripts/e2e-lint.sh` checks every flow against the
  rules in [`.maestro/README.md`](.maestro/README.md), and
  `./scripts/e2e-soak.sh <flow>` runs a flow five times from a cleared state
  before it can join the suite.
- **Signed-in flows** — need the backend running with dev routes mounted and
  seeded. `./scripts/e2e-up.sh` does all of it: starts your local Postgres
  container if it's down, starts the server with `-DSTREAM_PROVIDER=fake`
  (Flyway migrates on startup), and seeds the E2E account, feed posts and
  listings. It reuses anything already running; `./scripts/e2e-up.sh --down`
  stops a server it started. See [`.maestro/README.md`](.maestro/README.md)
  for how the session is injected.

### Maestro MCP (agent-driven E2E)

Maestro ships its own MCP server, which lets Claude Code inspect the screen,
run flows and read failures directly. The `scent-e2e` skill in
`.claude/skills/` drives that loop.

- **Register it** — either run `claude mcp add maestro -- maestro mcp`, or add
  it to the same gitignored `.mcp.json` that holds the HotSwan config:
  ```json
  {
    "mcpServers": {
      "maestro": { "command": "maestro", "args": ["mcp"] }
    }
  }
  ```
  If Claude Code can't find the binary, use the full path to `maestro`.
- **Check it** — with an emulator running, the `list_devices` tool should
  show it. `inspect_screen`, `run` and `take_screenshot` are the tools the
  loop leans on.

## Architecture

Scent follows a clean, layered architecture documented in `docs/architecture-guidelines.md`.

- **Data layer** — DTOs, mappers, remote/local data sources, repositories returning `Either<AppError, T>`
- **Domain layer** — non-null models, use cases, typed error hierarchy
- **UI layer** — Compose screens driven by `StateFlow<UiState<T>>` ViewModels; state-based navigation

## Contributing

Issues and pull requests are welcome. Please open an issue first to discuss larger changes.

## Licence

[MIT](LICENSE)
