# Tasks Tracker — full schema reference

Database: https://app.notion.com/p/30e11d6b186b801788d1eebab41e2194
Data source URL: `collection://30e11d6b-186b-80be-876e-000bf0145284`

## Properties (SQLite-style column view)

```sql
CREATE TABLE "collection://30e11d6b-186b-80be-876e-000bf0145284" (
    url TEXT UNIQUE,
    createdTime TEXT,               -- ISO-8601, auto-set
    "date:Due date:start" TEXT,     -- ISO-8601 date/datetime
    "date:Due date:end" TEXT,       -- for date ranges only, else NULL
    "date:Due date:is_datetime" INTEGER,
    "Priority" TEXT,                -- one of ["High", "Medium", "Low"]
    "Task type" TEXT,               -- JSON array, zero or more of ["🐞 Bug", "💬 Feature request", "💅 Polish"]
    "Description" TEXT,
    "Status" TEXT,                  -- one of ["Not started", "In progress", "Done"]
    "Assignee" TEXT,                -- JSON array of user IDs
    "Effort level" TEXT,            -- one of ["Small", "Medium", "Large"]
    "Task name" TEXT
)
```

## Notes

- `Status` is a Notion "status" property (not select) — it has grouping semantics (`to_do` → Not started, `in_progress` → In progress, `complete` → Done). Set the literal string value; Notion handles the group.
- `Task type` is multi-select — pass an array even for a single value, and the emoji must be included verbatim (e.g. `["🐞 Bug"]`), not just the word "Bug".
- `Past due` is a read-only formula property — never try to set it directly.
- There's a default page template ("New task") at https://app.notion.com/p/30e11d6b186b80898a81d8f52b3f30d8 if you need to see the intended shape of a blank ticket.

## Existing views

| View | Type | Notes |
|---|---|---|
| All Tasks | table | every property visible |
| By Status | board | grouped by Status — this is the "Jira board" view |
| My Tasks | table | filtered to the current user as Assignee |
