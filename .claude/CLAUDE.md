# Scent — Project Instructions

Scent is a fragrance social-commerce app: a short-form video feed fused with a
marketplace for buying, selling, and decanting fragrances. Kotlin Multiplatform
(`shared/`, `composeApp/`, `server/`). Deployed on Render free tier + Supabase.

## Authoritative docs

These are contracts. When code and doc disagree, the doc wins; if you think the
doc is wrong, say so explicitly rather than silently deviating.

- `docs/architecture-guidelines.md` (**ADS-STE100**) — read it fully, not just
  the section that looks relevant; the patterns interact.
- `docs/design.md` — design system and token names.
- `docs/auth.md` — auth roadmap: JWT (current) → Google OAuth → Apple Sign-In.
- `docs/adr/` — accepted ADRs bind like ADS-STE100.

## Working rules the docs don't capture

- Run `scent-dev-loop` by default on non-trivial work; don't stop at "it looks right."
- Accessibility goes into the ticket that builds the screen, never a follow-up.
- A gap found outside the current scope gets a `TODO(<type>/<kebab-slug>):` at
  every call site (slug = proposed follow-up branch) and a Tasks Tracker ticket
  in the same PR, not a bigger PR. A bare `// TODO:` is only for something fixed
  in the same PR.

## HotSwan MCP — hot reload

When the server is connected, use it for all Compose UI iteration: edit →
`hotswan_reload([<file>])` → `hotswan_take_screenshot` → decide.

- Check `hotswan_get_status`, then `hotswan_start` until status is `WATCHING`.
- **Install with the flag.** `composeApp/build.gradle.kts` only applies the
  `hotswan.compiler` plugin when `hotswanEnabled` is set (it breaks `allTests`
  on a bare JVM). Without it the app has no HotSwan runtime and every reload
  fails with "no hot-reload-enabled Gradle module" or "app closed the
  connection without replying". Whenever HotSwan is needed, install with
  `./gradlew :composeApp:installDebug -PhotswanEnabled=true`, then launch
  `org.scent.project/.MainActivity`. `hotswan_build_and_install` did **not**
  fix this when tried; prefer the Gradle command. Never add the flag to
  `gradle.properties`, since it would apply to the pre-push gate too.
- Verify the runtime is up: `adb shell run-as org.scent.project ls files`
  should list `hotswan_v2_port`, and logcat should show `HotSwanV2: v2 server
  listening`. Re-install with the flag after any plain `installDebug` or
  Android Studio Run, which replace the instrumented APK.
- The file watcher auto-applies literal edits, so a manual `hotswan_reload`
  straight after an edit can race it and report a connection error even though
  the change landed. Check logcat (`literal-patch: applied`) or screenshot
  before treating it as a failure.
- Reload *after* editing, one logical change per reload. On failure read
  `hotswan_get_logs`.
- Screenshot **only** when the change is complete, never a broken intermediate.
- Structural changes (new function param, class hierarchy, inline function)
  trigger a full incremental build. That is expected; let it finish.
- Scent uses a **custom theme data class**, so `hotswan_explore_palette` can't
  auto-detect it. Always go manual: baseline screenshot → per variant edit
  literals, reload, screenshot, `hotswan_revert_change` → `hotswan_show_palette_grid`
  with find/replace edits so each card is click-to-apply.

## Git & PR conventions

- **Branch:** `<type>/<kebab-case-description>`; type from the ticket's Task
  type: Feature request → `feature/`, Bug → `fix/`, everything else → `chore/`.
- Never commit directly to `main`.
- **Commits:** conventional prefixes (`feat`, `fix`, `refactor`, `docs`,
  `chore`, `test`, `perf`), imperative mood.
- **Updating a branch:** `git fetch origin && git rebase origin/main`. Never
  merge `main` into a feature branch.
- **Merging:** `gh pr merge --auto --squash --delete-branch`, so GitHub merges
  only once checks pass. Never merge immediately or bypass checks from the CLI.
- **PR body:** `### What` (one bullet per logical change, max 6; more means the
  PR is too big, say so) / `### Why` / `### Notes` (only if needed). Read
  `git diff main...HEAD` first; no filler bullets.
- **Pre-push gate:** `./gradlew ktlintCheck detekt allTests` must exit BUILD
  SUCCESSFUL. A Stop hook runs it at the end of turns that change Kotlin/Gradle files.

## Notion tracker

- Starting work on a linked ticket → Status `In progress` **before** writing code.
- PR merged → Status `Done`. The merge hook misses merges done on GitHub, so
  close those by hand.

## Local dev notes

- Android emulator can't reach `localhost`; use `10.0.2.2:8080`.
- Run the backend with `:server:run` (Android Studio, or Gradle panel under
  `server → Tasks → application → run`).
- Postgres 15+ needs explicit `GRANT ALL ON SCHEMA public` for the app user.
- Server config comes from `application.conf` via env vars: `JWT_SECRET`,
  `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.
- **Worktrees have no `.env`** (gitignored), so `:server:run` can't find the
  database config. From the worktree root: `ln -s <main-checkout>/.env .env`.
- **Maestro E2E** needs a running emulator (`emulator -avd Pixel_8`) and the
  seeded backend: `./scripts/e2e-up.sh` (`--down` stops it). The
  `scripts/e2e-*.sh` helpers find Maestro in `~/.maestro/bin` even when it
  isn't on `PATH`.

## How to work with me

- **Explain trade-offs, don't just pick.** I'm using this project to get
  properly good at Android/KMP. On a real decision (Room vs SQLDelight,
  state-based nav vs Nav3, sealed hierarchy shape), lay out the options and why
  one wins here specifically.
- **Push back when I'm wrong.** If I ask for something that violates ADS-STE100
  or fights the architecture, say so before building it.
- **Don't over-scaffold.** Build the thing asked for. Flag adjacent work as a
  ticket rather than silently expanding scope.
- **When a change is big, plan first.** Show the file-by-file shape before
  writing, so I can redirect cheaply.
