# Tasks Tracker — full schema reference

Database: `https://app.notion.com/p/<YOUR_DATABASE_PAGE_ID>`
Data source URL: `collection://<YOUR_COLLECTION_ID>`

## Properties (SQLite-style column view)

```sql
CREATE TABLE "collection://30e11d6b-186b-80be-876e-000bf0145284" (
    url TEXT UNIQUE,
    createdTime TEXT,               -- ISO-8601, auto-set
    "date:Due date:start" TEXT,     -- ISO-8601 date/datetime
    "date:Due date:end" TEXT,       -- for date ranges only, else NULL
    "date:Due date:is_datetime" INTEGER,
    "Priority" TEXT,                -- one of ["High", "Medium", "Low"]
    "Acceptance Criteria" TEXT,     -- newline-separated checklist, required on every new ticket
    "Task type" TEXT,               -- JSON array, zero or more of ["🐞 Bug", "💬 Feature request", "💅 Polish", "📝 Docs", "🔧 Tech Task"]
    "Description" TEXT,
    "Status" TEXT,                  -- one of ["Not started", "In progress", "In testing", "Done"]
    "Feature Branch" TEXT,          -- proposed or actual branch/PR, required on every new ticket
    "Assignee" TEXT,                -- JSON array of user IDs
    "Effort level" TEXT,            -- one of ["Small", "Medium", "Large"]
    "Task name" TEXT
)
```

## Notes

- `Status` is a Notion "status" property (not select) — it has grouping semantics (`to_do` → Not started; `in_progress` → In progress, In testing; `complete` → Done). Set the literal string value; Notion handles the group. `In testing` sits in the "In progress" group, ordered after `In progress`, so the board reads Not started → In progress → In testing → Done.
- Status options **cannot be added or renamed via the API / DDL** (`ALTER COLUMN "Status" SET STATUS(...)` is rejected). New status values must be added by hand in the Notion UI (Status property → Add option), then reflected here.
- `Task type` is multi-select — pass an array even for a single value, and the emoji must be included verbatim (e.g. `["🐞 Bug"]`), not just the word "Bug".
- `Acceptance Criteria` and `Feature Branch` were added after the first batch of tickets — the original 19 tickets don't have them backfilled. Only new tickets are required to have both; don't retroactively edit old tickets unless asked.
- `Past due` is a read-only formula property — never try to set it directly.
- There's a default page template ("New task") at `https://app.notion.com/p/<YOUR_TEMPLATE_PAGE_ID>` if you need to see the intended shape of a blank ticket.

## Existing views

| View | Type | Notes |
|---|---|---|
| All Tasks | table | every property visible |
| By Status | board | grouped by Status (Not started / In progress / In testing / Done) — this is the "Jira board" view |
| My Tasks | table | filtered to the current user as Assignee |
