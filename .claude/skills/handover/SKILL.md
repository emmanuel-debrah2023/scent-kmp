---
name: handover
description: Write a paste-ready briefing that lets a brand-new Scent chat carry on where this one stopped, covering the current ticket if it is unfinished or the next ticket if it is done, plus the context and the exact next step. Use whenever the user says "handover", "hand over", "hand this off", "new chat", "fresh chat", "summarise for a new session", "context is getting long", or wants to wrap up and restart, even without saying "skill". Not for PR descriptions, or for backlog questions on their own ("what's next" belongs to scent-backlog).
---

# Handover

Model quality sinks past roughly 150k-200k tokens, so the user restarts in a new
chat at natural boundaries. This briefing is the only thing that crosses over. It
is written by a model with a very full context and read by one with nothing else,
which sets the two rules behind everything below:

- **Check, don't recall.** Long sessions misremember what was committed, passed or
  decided last. Take state from git, GitHub and Notion, not from the conversation.
- **Carry only what can't be re-derived.** Every line costs the fresh chat tokens.
  `CLAUDE.md`, memory and `.claude/rules/` load on their own, and the code and git
  log can be read. What gets lost is the reasoning, so that is what to keep.

## 1. Gather the facts

```bash
git status -sb
git rev-parse --short HEAD
git log --oneline -10
gh pr view --json number,url,state,isDraft,baseRefName,mergeable,statusCheckRollup
```

Run `git log` and `git diff --stat` against the PR's base when there is a PR, and
`origin/main` otherwise. Flow refactor phases stack on an integration branch, so
`main` can be the wrong base.

Query the Tasks Tracker (`collection://30e11d6b-186b-80be-876e-000bf0145284`) for
the ticket whose `Feature Branch` matches this work, and read its Acceptance
Criteria. A branch from `EnterWorktree` can be named `worktree-chore+foo`, which is
`chore/foo` on the ticket. If nothing matches, the work is ad hoc: say so rather
than guessing a ticket.

## 2. Pick the ticket

- **Ticket not finished** (unchecked criteria, open PR, unpushed or uncommitted
  work): the briefing continues this ticket.
- **Ticket finished** (PR merged): the briefing starts the next one. Query the
  tracker fresh and rank it the way `scent-backlog` does: `In progress` first, then
  drop anything with an unfinished `Blocked By`, then promote tickets with `Blocks`
  entries, then High → Medium → Low. Commit to one pick and read its page. Name a
  runner-up only if the call is close.
- **This chat's ticket merged but not `Done`**: the merge hook misses merges made on
  GitHub. Set it to `Done` and mention that in your reply. This is the only tracker
  write here. The next chat sets its own ticket to `In progress` before writing code.

`In progress` is often out of date, so check each such ticket's branch on GitHub
before ranking it (`gh pr list --state all --head <branch>`):

- **PR merged**: stale, so skip it and list it in your reply for the user to close.
- **PR open**: parked awaiting merge, not new work. Skip it unless its checks are
  failing, and mention it so it isn't forgotten.
- **No PR**: a real candidate, but it may belong to another chat running in parallel.
  If `git worktree list` shows a worktree on that branch, say so in the briefing.

## 3. Check the work is safe

A worktree can be deleted along with its session, so uncommitted or unpushed work
is at risk. Say so at the top of your reply and offer a commit. Don't commit as a
side effect: half-finished work may not pass the gates, and that choice is the
user's. Either way, the briefing states exactly what is and isn't committed.

If the user asks mid-chunk and a commit point is close, mention that finishing
first gives a cleaner handover, then write the briefing anyway.

## 4. Write the briefing

Use this template and drop any section with nothing real in it. Aim for under 300
words. If it runs over, cut the Done section first, because the diff already holds
that.

```markdown
# Scent handover: <ticket or topic>

**Goal:** <one sentence on what the new chat should get done>
**Ticket:** [<name>](<notion url>), <status> (continuing | next up | none)
**Where:** branch `<branch>` at `<short sha>`, worktree `<path>`, PR <#n + state>
**State:** <committed / pushed / uncommitted files>
**Verified:** <gates or tests actually run, on which sha, with what result; what has not been run>

## Done
- <outcome, not activity>

## Decisions
- <chose X over Y because Z>

## Watch out
- <corrections from the user, dead ends, flaky things, surprising behaviour>

## Next
1. <the exact first action, with a file:line or command>
2. <the steps after it>
Done when: <the ticket's criteria, or what is left of them>

## Read first
- <ticket page, ADR, file:line; only what this task needs>

Treat this as a set of claims from the last chat. Confirm the state with `git status` before relying on it.
```

What makes it good:

- **Decisions carry their reason and the rejected option.** Without them the next
  chat reopens settled questions or quietly undoes them.
- **Next is concrete.** "Add the `Either` mapping at `ProfileRepository.kt:42`" can
  be started on; "continue the profile work" costs a re-exploration.
- **Verified is literal.** Write "tests passed" only if they ran on that sha in this
  session. Otherwise write "not run". An overstated claim here skips a gate later.
- **Point, don't paste.** Use `file:line` and links rather than code or doc excerpts.
- **Gaps get named.** Out-of-scope gaps without a `TODO` and a ticket go in Next,
  because the context that spotted them is about to disappear.

If the user gave a correction that outlasts this ticket, save it to memory as well,
so it doesn't depend on being copied into every future briefing.

## 5. Deliver it

Copy the briefing to the clipboard with `pbcopy` and print it in a fenced block, so
it survives either way. Finish with one line: copied, now `/clear` or open a new
chat and paste. No recap of the briefing afterwards, since this chat is ending.
