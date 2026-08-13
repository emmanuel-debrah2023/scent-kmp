---
name: scent-ticket
description: Create or triage tasks/tickets for the Scent app (the Kotlin Multiplatform fragrance social-commerce project) in its Notion "Tasks Tracker" database. Use this whenever the user wants to log a task, ticket, bug, feature request, or action item for Scent / Scent KMP — phrases like "create a ticket for...", "add this to the tracker", "turn these into tasks", "log these action items", "add a card for...", or when they hand you a list of engineering to-dos (e.g. from a code review, ADR, or planning session) that need to land in the project's backlog. Also use this to pull up a Jira-style board view of the current backlog. Don't wait for the user to say "Notion" explicitly — if the work clearly belongs to the Scent project, this skill applies.
---

# Scent Ticket

Creates rows in the Scent project's Notion **Tasks Tracker** database so engineering work (code review findings, ADR action items, feature ideas, bugs) doesn't stay stuck in a chat transcript.

**CRITICAL**: Every ticket created must include ADS-STE100 compliance as part of its acceptance criteria. ADS-STE100 (`docs/architecture-guidelines.md`) is the authoritative design doc for the Scent codebase — it covers null-safety strategy, error handling with `Either<AppError, T>`, Koin dependency injection, ViewModel/UiState patterns, test conventions, and navigation architecture. When creating acceptance criteria, always include a checkpoint that work passes ADS-STE100 review before it's marked done.

**Database:** https://app.notion.com/p/30e11d6b186b801788d1eebab41e2194
**Data source URL (use this in create/update/query tool calls):** `collection://30e11d6b-186b-80be-876e-000bf0145284`
**Jira-style board view (grouped by Status):** https://app.notion.com/p/30e11d6b186b8056b2ea000ce7308558

Full schema detail lives in `references/schema.md` — read it if you need exact property names or option values beyond what's summarized below.

## Schema summary

| Property | Type | Values |
|---|---|---|
| Task name | title | free text (required) |
| Description | text | free text |
| Status | status | `Not started` (default) / `In progress` / `Done` |
| Priority | select | `High` / `Medium` / `Low` |
| Task type | multi-select | `🐞 Bug` / `💬 Feature request` / `💅 Polish` / `📝 Docs` / `🔧 Tech Task` (exact strings incl. emoji — must match verbatim) |
| Effort level | select | `Small` / `Medium` / `Large` |
| Due date | date | optional |
| Assignee | person | optional — only set if the user names someone |
| Acceptance Criteria | text | required on every new ticket — see below |
| Feature Branch | text | required on every new ticket — see below |

## Creating tickets

1. For each item to log, work out: a short **Task name**, a one-to-two sentence **Description** giving enough context that it's actionable without the original conversation, and best-guess **Priority** / **Task type** / **Effort level**. Don't leave these blank just because the user didn't specify — infer sensible defaults from context (e.g. a security/data-integrity fix from a code review is `High` priority; a "nice to have" is `Low`).
2. `Task type` is a multi-select with a fixed vocabulary. Map freely:
   - New capability the app doesn't have yet → `💬 Feature request`
   - Something broken or incorrect → `🐞 Bug`
   - Visual/UX refinement, small polish pass on existing UI → `💅 Polish`
   - README, architecture docs, runbooks, comments-as-documentation, anything whose deliverable is written explanation rather than code → `📝 Docs`
   - Infra/tooling/config/refactor work a developer would do but a user would never notice directly — linting, static analysis, test setup, CI, dependency upgrades, splitting a god-class, extracting a shared module → `🔧 Tech Task`
   - When something could plausibly be either Polish or Tech Task, ask: would a non-engineer notice this shipped? If no, it's Tech Task.
3. **Write Acceptance Criteria on every ticket, no exceptions.** This is what turns a one-line task name into something checkable later — write it as a short newline-separated checklist of concrete, testable conditions, not a restatement of the description. Aim for 2-5 bullets. **Always include an ADS-STE100 compliance checkpoint** (references Scent's authoritative architecture guideline, `docs/architecture-guidelines.md`). Bad: "Works correctly." Good:
   ```
   - Video screen renders without crashing on both Android and iOS targets
   - Playback controls (play/pause/seek) respond within one frame
   - Unit tests cover the ViewModel's loading/error/success states
   - Complies with ADS-STE100 (null-safety boundary, Either error handling, Koin DI, UiState pattern)
   - ktlint and detekt pass with no new violations
   ```
   If the ticket came from an ADR action item or code-review finding, the "problem" described there usually implies the AC directly (e.g. "resolve the duplicate AuthModels.kt" → AC is "only one AuthModels.kt exists" + "server module still compiles" + "passes ADS-STE100 review"). Don't skip ADS-STE100 compliance even for small tickets — it's the contract for the codebase.

   **New UI screens and components must state how accessibility is handled in the same AC — never as a follow-up ticket.** If the ticket builds or changes a Composable screen, card, button, toggle, or other UI component, add at least one concrete accessibility line using the shared `ui/accessibility` Modifier extensions (`accessibleLabel`, `accessibleClickable`, `mergedGroup`, `accessibleToggle`, `clearedDescription`, `withCustomActions`, `collectionContainer`/`collectionItem`, `accessiblePane`, `accessibleState` — see `docs/architecture-guidelines.md`'s Accessibility section for which one fits). Bad (accessibility as its own bullet with no substance): "- Accessible." Good, folded into a screen-build ticket's AC alongside the rest:
   ```
   - MarketplaceScreen renders a LazyColumn of ListingCards from UiState<List<Listing>>
   - ListingCard uses clearedDescription for one clean "name, condition, price" announcement; decorative badge icons marked non-semantic
   - LazyColumn uses collectionContainer/collectionItem semantics
   - Complies with ADS-STE100
   ```
   Don't spin up a second "make X accessible" ticket for a screen or component that hasn't shipped yet — that's exactly the afterthought pattern this rule exists to avoid. A standalone accessibility ticket is only appropriate for *retrofitting* accessibility onto UI that's already built and shipped (check the repo — if the screen/component doesn't exist in `composeApp/src/commonMain/kotlin/ui` yet, fold the AC into whichever ticket builds it instead of creating a new one). If you're about to create a ticket like "M2: Wire X screen..." or "Build Y component," write the accessibility AC into it directly rather than defaulting to a separate pass.

   **Any ticket touching testable logic must include a concrete unit-test AC line — not just the ADS-STE100 checkpoint.** ADS-STE100 compliance covers *how* the code is structured (Either, UiState, DI); it doesn't by itself guarantee tests exist. Applies to ViewModels, use cases, repositories, mappers, validators, and Ktor routes — anything `docs/architecture-guidelines.md`'s test-coverage bar names (ViewModels 80%+, use cases 100%, repositories 90%+, validation/error-mapping 100%). Name what's actually being tested, not a generic "add tests" bullet:
   ```
   - MarketplaceViewModel has tests for loading, success, and error UiState transitions (Turbine)
   - GetListingsUseCase has a test for the empty-page and validation-failure paths
   ```
   Bad: "- Tests added." Good tickets say *what* behavior the tests pin down, mirroring the "state how accessibility is handled" rule above rather than a bare checkbox. Skip this line only for tickets with no testable logic at all — pure docs, config/tooling with no new code path, or copy/asset changes.
4. **Set Feature Branch on every ticket.** Even before a branch exists, propose one following the repo's existing convention (`<type>/<kebab-case-description>`, e.g. `feature/video-screen`, `fix/auth-token-refresh`, `chore/introduce-ktlint-detekt`), derived from the Task type:
   - `💬 Feature request` → `feature/...`
   - `🐞 Bug` → `fix/...`
   - `💅 Polish`, `📝 Docs`, `🔧 Tech Task` → `chore/...`
   Once real work starts and an actual branch (or PR) exists, update this field to the real branch name or PR URL rather than leaving the proposed one stale — treat the proposed name as a placeholder the assignee should use, not a permanent record of what was actually pushed.
5. Create pages via the Notion connector's page-creation tool, targeting the data source URL above. Set `Status` to `Not started` unless told otherwise. Leave `Assignee` and `Due date` unset unless the user specifies them — don't guess a person or date.
6. When creating several tickets at once (e.g. from a list of action items), create them one at a time but batch the work — don't stop to ask about each one individually unless something is genuinely ambiguous (e.g. you can't tell if two bullet points are one ticket or two).
7. After creating, report back a short list of what was created (task name + inferred type/priority + proposed branch) and link to the board view so the user can see them land in the right column.

## Showing the board

If the user just wants to see current state rather than add anything, point them at the board view URL above (grouped by Status: Not started / In progress / Done — mirrors a Jira board's To Do / In Progress / Done columns) rather than recreating one from scratch. If they want a richer or differently-grouped visual (e.g. grouped by Task type, or an artifact that also shows priority color-coding), build an HTML kanban artifact by querying the data source and grouping client-side — see the main app's guidance on artifacts for live/persisted views.

## Lifecycle status updates

Keep Notion status in sync with what's actually happening — don't leave tickets stale.

### Starting work on a ticket
Whenever the user says to work on / implement / pick up a ticket and links a Notion URL:
1. Fetch the ticket with the Notion fetch tool.
2. If the current `Status` is anything other than `In progress`, update it to `In progress` immediately — before doing any engineering work.
3. Confirm the status change to the user in one line, then proceed with the implementation.

### Merging a PR that corresponds to a Notion ticket
Whenever a PR is merged (or the user says "merge the MR / PR") and there is a Notion ticket linked in the conversation:
1. After the merge succeeds, update the ticket's `Status` to `Done`.
2. Confirm the status change to the user in one line.

Use the Notion `update-page` tool with the ticket's page ID to set the `Status` property. The exact value strings are `In progress` and `Done` (match verbatim).

## Answering "what's next"

Whenever the user asks something like "what's next", "what work is next", "what should I work on", or "what's on the backlog" — query the tracker fresh rather than answering from memory of this conversation; tickets change between sessions.

1. Query the data source (or the "All Tasks" view) and drop anything with `Status = Done`.
2. Anything already `In progress` goes first, regardless of Priority — an unfinished ticket takes precedence over starting something new. Flag it clearly (e.g. "already in progress") so it doesn't read like a fresh suggestion.
3. After that, sort the remaining `Not started` tickets by `Priority` (High, then Medium, then Low).
4. Return the result as a flat bulleted list, not a table — one line per ticket: **Task name** (Task type emoji, Priority, Feature Branch if set). Lead with the `In progress` item(s) if any, then High priority, and so on.
5. Keep it to the tickets that actually matter right now — the High priority tier plus anything in progress is usually enough. Only spill into Medium/Low if High is empty, and say so explicitly ("nothing else is High priority, next up:") rather than silently padding the list.
6. Don't editorialize with a big prose recommendation unless asked — the point of this is a fast, scannable answer, not another architecture discussion.