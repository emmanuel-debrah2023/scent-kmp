---
name: scent-ticket
description: Create or triage tasks/tickets for the Scent app (the Kotlin Multiplatform fragrance social-commerce project) in its Notion "Tasks Tracker" database. Use this whenever the user wants to log a task, ticket, bug, feature request, or action item for Scent / Scent KMP — phrases like "create a ticket for...", "add this to the tracker", "turn these into tasks", "log these action items", "add a card for...", or when they hand you a list of engineering to-dos (e.g. from a code review, ADR, or planning session) that need to land in the project's backlog. Also use this to pull up a Jira-style board view of the current backlog. Don't wait for the user to say "Notion" explicitly — if the work clearly belongs to the Scent project, this skill applies.
---

# Scent Ticket

Creates rows in the Scent project's Notion **Tasks Tracker** database so engineering work (code review findings, ADR action items, feature ideas, bugs) doesn't stay stuck in a chat transcript.

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
3. **Write Acceptance Criteria on every ticket, no exceptions.** This is what turns a one-line task name into something checkable later — write it as a short newline-separated checklist of concrete, testable conditions, not a restatement of the description. Aim for 2-5 bullets. Bad: "Works correctly." Good:
   ```
   - Video screen renders without crashing on both Android and iOS targets
   - Playback controls (play/pause/seek) respond within one frame
   - Unit tests cover the ViewModel's loading/error/success states
   - ktlint and detekt pass with no new violations
   ```
   If the ticket came from an ADR action item or code-review finding, the "problem" described there usually implies the AC directly (e.g. "resolve the duplicate AuthModels.kt" → AC is "only one AuthModels.kt exists" + "server module still compiles"). Don't skip this step because the ticket seems obvious — the point is to remove ambiguity about what "done" means before anyone starts, including future-you.
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
Whenever a PR is merged (or the user says "merge the MR / PR") and there is a Notion ticket linked in the conversation, do all of the following after the merge succeeds:

1. Set `Status` to `Done` via `update-page`.
2. Write a post-merge summary into the ticket's page body using `insert_content` (append to end). Always include:
   - **PR link** — the GitHub PR URL (e.g. `[PR #16](https://github.com/...)`).
   - **Scope creep** — if anything was built that wasn't in the original ticket description or acceptance criteria, list it explicitly under a "Scope creep" heading. Be specific: name the files/classes/layers added beyond scope. If there was no scope creep, omit this section entirely — don't write "No scope creep."
   - **Unforeseen issues** — if any blockers, surprises, or workarounds came up during implementation (e.g. missing dependencies, pre-existing test failures, platform limitations, architectural gaps), document them under an "Unforeseen issues" heading. If none, omit the section.
3. Confirm the status change and page update to the user in one or two lines.

Use this markdown structure for the page body insert — only include sections that apply:

```
## Merge summary

**PR:** [PR #N — branch-name](https://github.com/emmanuel-debrah2023/scent-kmp/pull/N)

### Scope creep
- <what was added beyond original scope and why>

### Unforeseen issues
- <what was discovered mid-implementation and how it was handled>
```

Use the Notion `update-page` tool with the ticket's page ID. Status value strings are `In progress` and `Done` (match verbatim).

## Answering "what's next"

Whenever the user asks something like "what's next", "what work is next", "what should I work on", or "what's on the backlog" — query the tracker fresh rather than answering from memory of this conversation; tickets change between sessions.

1. Query the data source (or the "All Tasks" view) and drop anything with `Status = Done`.
2. Anything already `In progress` goes first, regardless of Priority — an unfinished ticket takes precedence over starting something new. Flag it clearly (e.g. "already in progress") so it doesn't read like a fresh suggestion.
3. After that, sort the remaining `Not started` tickets by `Priority` (High, then Medium, then Low).
4. Return the result as a flat bulleted list, not a table — one line per ticket: **Task name** (Task type emoji, Priority, Feature Branch if set). Lead with the `In progress` item(s) if any, then High priority, and so on.
5. Keep it to the tickets that actually matter right now — the High priority tier plus anything in progress is usually enough. Only spill into Medium/Low if High is empty, and say so explicitly ("nothing else is High priority, next up:") rather than silently padding the list.
6. Don't editorialize with a big prose recommendation unless asked — the point of this is a fast, scannable answer, not another architecture discussion.
