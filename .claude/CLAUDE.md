# Scent — Project Instructions

## What this is

Scent is a fragrance social-commerce app: a short-form vertical video feed
(TikTok-style) fused with a marketplace for buying, selling, and decanting
fragrances.

- **Repo:** github.com/emmanuel-debrah2023/scent-kmp
- **Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor, Exposed, PostgreSQL
- **Modules:** `shared/` (domain + data), `composeApp/` (UI, commonMain/androidMain/iosMain), `server/` (Ktor backend)
- **Deploy:** Render free tier (backend) + Supabase (Postgres)
- **DI:** Koin

---

## Authoritative docs — read before writing code

These are contracts, not suggestions. When code and doc disagree, the doc wins;
if you think the doc is wrong, say so explicitly rather than silently deviating.

| Doc | Covers |
|---|---|
| `docs/architecture-guidelines.md` (**ADS-STE100**) | Null-safety boundary, `Either<AppError, T>`, Koin DI, `UiState` pattern, per-tab navigation, test conventions, accessibility |
| `docs/design.md` | Scent Minimalist Luxury design system — M3 token names, Playfair Display / DM Sans, colour role separation |
| `docs/auth.md` | Auth roadmap: Phase 1 JWT (current) → Phase 2 Google OAuth → Phase 3 Apple Sign-In |

**Read ADS-STE100 fully, not just the section that looks relevant.** The patterns
interact — null-safety affects mapping, mapping affects error handling, error
handling affects ViewModel design. Skipping it causes rework.

---

## Non-negotiables

These come up constantly. Getting them wrong means a redo.

- **DTOs are nullable, domain models are not.** Every field on a `*Dto` is
  nullable with a default. Domain models are non-null with sensible defaults
  (`emptyList()`, `0`, `""`). Nullable in a domain model only when absence is
  semantically meaningful (e.g. `releaseYear`).
- **Mapping returns `Either<AppError, T>`.** Mappers live in `data/mapper/`,
  never inline in a repository or ViewModel. `filterNotNull()` on nullable lists.
- **No exceptions for expected failures.** Network errors, validation failures,
  not-found — all `Either.Left`. Exceptions only for genuinely exceptional cases.
- **No `!!`.** Ever.
- **Constructor injection only.** No `get<T>()` inside class bodies.
- **Design tokens, not literals.** No hardcoded hex or dp values in Composables —
  use the M3 token names from `design.md`. Note the three-way colour role split:
  `primary` (green, brand/structure), `accent` (gold, decoration only, never text
  on cream), `interactive` (gold-brown, inline links). Don't collapse them.
- **Accessibility is written into the ticket that builds the screen**, never
  deferred to a follow-up. Use the `ui/accessibility` Modifier extensions.

---

## Skills — when they fire

- `scent-dev-loop` — implementing/fixing/refactoring anything in this repo.
  Five gates: compiles → ADS-STE100 → ktlint/detekt → unit tests → pre-push.
  Run it by default on non-trivial work; don't stop at "it looks right."
- `scent-ticket` — logging tasks to the Notion Tasks Tracker.
- `scent-backlog` — "what's next", read-only backlog query.
- `db-backend-ktor` — smoke-testing endpoints, local Postgres, Ktor tests, Flyway.
- `mock-interview` — practising system design grounded in real commits.
- `advisor-executor` — large refactors/migrations where premium model budget matters.

---

## HotSwan MCP — hot reload workflow

When the HotSwan MCP server is connected, use it for all Compose UI iteration.
This closes the feedback loop: edit → reload → see a real device screenshot → decide.

### Loop

1. `hotswan_get_status` — confirm device connected, app installed.
2. `hotswan_start` — status should reach `WATCHING`.
3. Edit the Compose file with normal file tools.
4. `hotswan_reload([<file_path>])`. On failure, check `hotswan_get_logs`, fix, retry.
5. `hotswan_take_screenshot` — **only** when the change is complete. Never
   screenshot intermediate or broken states.
6. Repeat 3–5 per variant, then `hotswan_select_variant(snapshotId, reason)`.

### Palette / design variants

Scent uses a **custom theme data class**, not `lightColorScheme`/`darkColorScheme`.
`hotswan_explore_palette` will not auto-detect it — **always use manual mode**:

1. Baseline `hotswan_take_screenshot`.
2. Per variant: edit the Color/dimension literals → `hotswan_reload` →
   `hotswan_take_screenshot` → `hotswan_revert_change`.
3. `hotswan_show_palette_grid(variants)` with find/replace edits included so
   each card is click-to-apply.

### Rules

- Reload *after* editing, never before.
- One logical change per reload — don't stack edits.
- Structural changes (new function param, class hierarchy change, inline function
  edit) fall back to a full incremental build automatically. Let it finish, then
  resume the loop. That's expected behaviour, not a failure.

---

## Git & PR conventions

- **Branch naming:** `<type>/<kebab-case-description>` — `feature/video-feed`,
  `fix/auth-token-refresh`, `chore/hotswan-mcp-server`. Type derives from the
  ticket's Task type: Feature request → `feature/`, Bug → `fix/`, everything
  else → `chore/`.
- **Never commit directly to `main`.** Always branch.
- **Commits:** conventional prefixes (`feat`, `fix`, `refactor`, `docs`, `chore`,
  `test`, `perf`), imperative mood — "Add auth routes", not "Added" or "Adds".
- **Updating a feature branch:** rebase onto `main`, don't merge `main` in.
  `git fetch origin && git rebase origin/main` keeps history linear and the PR
  diff clean. Never `git merge main` into a feature branch — it creates merge
  commits that clutter the diff and make the PR harder to review.
- **Merge strategy:** `gh pr merge --squash --delete-branch`.
- **PR body format:**
  ```
  ### What
  - one bullet per logical change, not per file (max 6)
  ### Why
  - business or technical reason
  ### Notes
  - breaking changes, env vars, migration steps — only if needed
  ```
  Read the actual diff (`git diff main...HEAD`) before writing it. No filler
  bullets like "minor tweaks" or "various improvements". If it needs more than
  six bullets, the PR is too big — say so.
- **Pre-push gate:** `./gradlew ktlintCheck detekt allTests` must exit
  BUILD SUCCESSFUL. Same bar CI enforces.

---

## Notion tracker

Tickets live in the Scent **Tasks Tracker**. Board view is grouped by Status
(Not started / In progress / Done).

- Starting work on a linked ticket → set Status to `In progress` **before**
  writing code.
- PR merged → set Status to `Done`.
- Every new ticket needs Acceptance Criteria (2–5 concrete testable bullets,
  including an ADS-STE100 compliance line) and a proposed Feature Branch.

---

## Local dev notes

- **Android emulator can't reach `localhost`** — use `10.0.2.2:8080`.
- Backend runs via `:server:run` in Android Studio, or the Gradle panel under
  `server → Tasks → application → run`.
- Postgres 15+ needs explicit `GRANT ALL ON SCHEMA public` for the app user —
  this bites on fresh local setups.
- Server config comes from `application.conf` via env vars: `JWT_SECRET`,
  `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.

---

## How to work with me

- **Explain trade-offs, don't just pick.** I'm using this project to get
  properly good at Android/KMP, not just to ship. When there's a real decision
  (Room vs SQLDelight, state-based nav vs Nav3, sealed hierarchy shape), lay out
  the options and why one wins here specifically.
- **Push back when I'm wrong.** If I ask for something that violates ADS-STE100
  or fights the architecture, say so before building it.
- **Don't over-scaffold.** Build the thing asked for. Flag adjacent work as a
  ticket rather than silently expanding scope.
- **When a change is big, plan first.** Show the file-by-file shape before
  writing, so I can redirect cheaply rather than after 400 lines exist.
