---
name: advisor-executor
description: Split reasoning from execution across Claude Code model tiers to stretch a premium model's (Opus/Fable 5) usage allocation on large, multi-step tasks. Use this whenever the user mentions a large refactor, a multi-file migration, a codebase-wide audit, batch test writing, or explicitly says things like "advisor executor pattern", "save my Fable 5 usage", "split planning and execution", "don't burn my Opus limit on this", or asks how to make a big task use fewer premium-model tokens. Also trigger when the user is about to run a large task end-to-end on a single expensive model and the task is the kind where planning is a small fraction of the work (migrations, refactors, systematic test generation) — proactively suggest splitting it before they burn their allocation.
---

# Advisor-Executor Pattern

The core idea, regardless of which mechanism you use: a frontier model (Opus, Fable 5) is expensive because it reasons well, not because it types code well. For most software tasks, the *planning* phase — understanding the problem, choosing an approach, spotting edge cases — is a small fraction of total tokens. The *execution* phase — actually writing/editing dozens of files — is most of the tokens, and a cheaper, faster model can follow a clear plan just as reliably. Splitting the two stretches your premium-model allocation across far more tasks.

There are two ways to actually do this in Claude Code. Pick based on the situation — don't default to the manual one just because it's more well-known from blog posts.

## Path A: the built-in advisor tool (default choice)

Claude Code has a real, native feature for this: the `/advisor` tool (v2.1.98+, Anthropic API billing only — not available on Bedrock, Vertex, or Foundry). You set a stronger advisor model once, and Claude's main model consults it automatically at key moments — before committing to an approach, when stuck on a recurring error, before declaring a task done — without you managing any files or switching sessions.

**Set it up:**
```
/advisor fable
```
or `/advisor opus`, or set `advisorModel` in your settings file for a persistent default, or pass `--advisor fable` at launch for one session. Requires Claude Code v2.1.170+ and Fable 5 access if you want Fable 5 specifically as the advisor.

**Model pairing rules** (the advisor must be at least as capable as the main model):
- Sonnet main + Opus or Fable advisor — routine work on Sonnet, escalate planning/failures/completion checks
- Opus main + Fable advisor — a step up for high-stakes tasks
- Fable main + Fable advisor only — Fable rejects Opus/Sonnet as an advisor since nothing outranks it

**Nudge it explicitly** if you want more control over timing: say "consult the advisor before you continue" or "check with the advisor before you call this done" in your prompt — there's no setting to force or cap advisor calls, only your own instructions.

**Use this by default** when you're working in one continuous session and are comfortable letting Claude decide when escalation is warranted. It's less setup, no plan file to manage, and it's a real product feature rather than a workaround.

## Path B: manual two-phase pattern (explicit control)

Use this instead when: you're on Bedrock/Vertex/Foundry (Path A isn't available there), you're on an older Claude Code version, or — most commonly — you specifically want to review and edit the plan yourself before any execution happens, rather than trusting Claude's judgment on when to escalate. This is the pattern from the MindStudio "advisor-executor" writeup: two separate sessions, connected by a plan file on disk.

### Step 1 — Advisor session (planning only)

Launch with the stronger model and ask for a plan, not work:

```
claude --model fable "Analyze <the codebase/files in scope> and produce a step-by-step plan for <the task>. For each unit of work, list: what needs to change, why, and any edge cases or risks. Output the plan as structured JSON/YAML at <path/to/plan.json>. Do not modify any source files — analysis only."
```

Two things matter here: **ask for structured output** (JSON/YAML/numbered list, not prose — an executor model needs to parse this reliably), and **explicitly forbid execution** ("do not modify any source files," "analysis only"). Without both, the advisor tends to start doing the work itself, which defeats the point and burns exactly the tokens you're trying to save.

### Step 2 — Executor session (follow the plan, don't re-derive it)

Launch a new session with the cheaper model, pointed at the plan:

```
claude --model sonnet "You have a plan at <path/to/plan.json>. Work through it in order. For each item, apply exactly what the plan specifies. If you hit something the plan didn't anticipate, don't improvise a fix — add a note explaining the issue and move to the next item. After completing each item, mark it done in the plan file so progress is resumable."
```

Key phrases worth keeping close to verbatim: **"don't deviate from the plan"** (stops the executor from re-strategizing, which is slower and can drift from what the advisor decided) and **"note issues and continue"** (keeps judgment calls with the advisor instead of letting the cheaper model improvise on things it's not equipped to decide).

### Step 3 — Spot-check and re-advise if needed

When the executor finishes or flags issues, open a short follow-up advisor session to review just the flagged items — not a full re-run of the whole task. This keeps the loop (advise → execute → spot-check → re-advise) concentrated on the parts that actually need frontier-level judgment.

## Common mistakes (either path)

- **Letting the advisor start executing.** Watch for your own prompts sliding from "plan the migration" into "plan the migration and start with the first file" — that bleeds execution into the expensive model. Keep advisor prompts to "analyze," "identify," "produce a plan," never "do," "convert," "write."
- **Vague executor prompts.** If the executor prompt doesn't say exactly how to handle edge cases and track progress, the cheaper model fills the gap with its own reasoning — slower, and prone to drifting from the plan.
- **Unstructured plans.** A wall of prose forces the executor to infer intent. Always require JSON/YAML/numbered output from the advisor phase.
- **Letting the executor make judgment calls.** If it hits something unexpected, it should log it, not decide. Bring genuinely ambiguous cases back to the advisor.

## Choosing between the two paths

| | Path A: `/advisor` | Path B: manual two-phase |
|---|---|---|
| Setup | One command, stays on for the session | Two separate CLI launches + a plan file |
| Who decides when to escalate | Claude, automatically | You, by defining the plan/execute boundary yourself |
| Best for | Ongoing work where you trust the model's judgment | Large, well-scoped batch jobs (migrations, systematic refactors) you want to review before execution |
| Availability | Anthropic API billing only, Code v2.1.98+ | Anywhere the CLI runs, any Code version |
| Review point | Mid-task, inline in the transcript (`Ctrl+O` to expand) | Explicit — you read the plan file before Step 2 starts |

## A caveat on the numbers

Efficiency figures like "93% of tokens on the executor" come from a single vendor's illustrative example (a 28-component React migration), not a guaranteed ratio — actual split depends heavily on how much genuine judgment the task requires. Treat it as "expect the execution phase to dominate token usage for well-scoped tasks," not as a number to plan a budget around.
