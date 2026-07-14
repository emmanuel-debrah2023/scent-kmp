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
| Task type | multi-select | `🐞 Bug` / `💬 Feature request` / `💅 Polish` (exact strings incl. emoji — must match verbatim) |
| Effort level | select | `Small` / `Medium` / `Large` |
| Due date | date | optional |
| Assignee | person | optional — only set if the user names someone |

## Creating tickets

1. For each item to log, work out: a short **Task name**, a one-to-two sentence **Description** giving enough context that it's actionable without the original conversation, and best-guess **Priority** / **Task type** / **Effort level**. Don't leave these blank just because the user didn't specify — infer sensible defaults from context (e.g. a security/data-integrity fix from a code review is `High` priority; a "nice to have" is `Low`).
2. `Task type` is a multi-select with a fixed vocabulary. Map freely:
   - New capability the app doesn't have yet → `💬 Feature request`
   - Something broken or incorrect → `🐞 Bug`
   - Cleanup, refactor, tooling, tech debt, "do it properly" work → `💅 Polish` (there's no dedicated "chore" option — Polish is the closest fit)
3. Create pages via the Notion connector's page-creation tool, targeting the data source URL above. Set `Status` to `Not started` unless told otherwise. Leave `Assignee` and `Due date` unset unless the user specifies them — don't guess a person or date.
4. When creating several tickets at once (e.g. from a list of action items), create them one at a time but batch the work — don't stop to ask about each one individually unless something is genuinely ambiguous (e.g. you can't tell if two bullet points are one ticket or two).
5. After creating, report back a short list of what was created (task name + inferred type/priority) and link to the board view so the user can see them land in the right column.

## Showing the board

If the user just wants to see current state rather than add anything, point them at the board view URL above (grouped by Status: Not started / In progress / Done — mirrors a Jira board's To Do / In Progress / Done columns) rather than recreating one from scratch. If they want a richer or differently-grouped visual (e.g. grouped by Task type, or an artifact that also shows priority color-coding), build an HTML kanban artifact by querying the data source and grouping client-side — see the main app's guidance on artifacts for live/persisted views.
