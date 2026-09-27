---
name: scent-e2e
description: |
  Write, run and repair Maestro end-to-end flows for the Scent Android app in an
  agent-driven loop: write the flow, lint it, run it, read the failure, fix the
  flow, and soak it before review. Use this whenever the task is an E2E test, a
  Maestro flow, a user-journey test, "test this screen end to end", a flaky or
  failing flow under .maestro/, or anything using the Maestro MCP tools. Not for
  unit or Compose UI tests (scent-tdd) or Kotlin changes (scent-dev-loop).
keywords: [maestro, e2e, flows, android, emulator, scent, mcp, soak]
---

# Scent E2E

The job: an agent writes a flow, runs it, reads the failure and fixes the flow
without a human in the loop, leaving only a diff to review. This skill sets
the loop and, more importantly, its boundaries.

Authoritative sources, read before the first flow in a session:
- `docs/adr/0002-e2e-testing-maestro.md` — what Maestro owns, the black-box and
  gray-box split, and the selector policy.
- `.maestro/README.md` — the flow rules the linter enforces.

## Hard boundaries

1. **Fix the flow, never the app.** When a flow fails, the only files the loop
   may edit are under `.maestro/`. If passing would need an app, server or
   build change, stop and report it (see "Real bugs" below). This includes
   adding a `contentDescription` or `testTag`: that is an app change and needs
   a human decision first.
2. **No weakening a flow to make it pass.** Deleting an assertion, loosening
   one to something always true, raising a timeout, adding `optional: true` or
   switching to an `id:` selector to get green is not a fix. Escape hatches
   need a `# UNSAFE: <reason>` comment stating why the app gives no better
   option, and the reason is reviewed like code.
3. **One black-box journey.** Only the auth flow drives the login and register
   screens. Every other flow starts with
   `- runFlow: ../subflows/start-authenticated.yaml`.

## Preconditions

Check these before the first run and report plainly if any is missing. Never
work around a missing one.

- An emulator is running (`list_devices` via the Maestro MCP, or `adb devices`).
- The debug APK is installed (`./scripts/e2e-local.sh` installs it).
- For signed-in flows, the backend is up with dev routes mounted:
  `./gradlew :server:run -DSTREAM_PROVIDER=fake`.

## The loop

For each flow:

1. **Explore.** Open the target screen and call `inspect_screen` to read the
   real hierarchy before choosing selectors. Use the MCP `run` tool with
   inline YAML to try individual steps. `cheat_sheet` covers syntax.
2. **Write** the flow in `.maestro/flows/<journey>.yaml`:
   - Start from `start-authenticated.yaml` unless this is the auth journey.
   - Select by visible text or accessibility label.
   - Put an assertion after every action, and refute the old screen before
     asserting the new one on each transition.
   - Open with a one-line comment stating what the flow proves.
3. **Lint** with `./scripts/e2e-lint.sh <flow>` before every run. A lint
   failure is fixed before running, never run around.
4. **Run** with `maestro test <flow>` (or `./scripts/e2e-local.sh <flow>`).
5. **Read the failure.** Use the failing step, its error, `take_screenshot`,
   `inspect_screen` and the debug output. Classify it before touching
   anything:

   | Symptom | Class | Action |
   |---|---|---|
   | Selector matches nothing, but the element is on screen under different text or label | Flow bug | Fix the selector |
   | A tap "completed" but hit the wrong node: a label instead of its input, or the keyboard | Flow bug | Target the input itself and assert the outcome (`focused: true`), not just that some text is visible |
   | Assertion ran before the screen settled | Flow bug | Add or move an `extendedWaitUntil` within the 15 s default |
   | Wrong screen because the flow skipped or mis-ordered a step | Flow bug | Fix the steps |
   | Precondition missing (no device, backend down, dev routes off) | Environment | Stop and report |
   | App shows an error, crashes, or behaves against the ticket, ADR or design | Real bug | Stop (see below) |
   | Element has no label of its own but sits between visible labels (e.g. an unlabelled input) | App gap | Reach it with a bounded relative selector (see `.maestro/README.md` rule 3), add a `TODO(fix/...)` and log the gap with `scent-ticket` |
   | Element the flow needs has no text or label anywhere near it | App gap | Stop and ask |

6. **Fix the flow** and go back to step 3.

**Retry ceiling.** At most three fix attempts for any one failure. Also stop
early if the same step fails the same way twice in a row after a fix. At the
ceiling, stop and report the flow, the failing step, each attempt made and why
it didn't work. Don't keep circling.

## Real bugs

When the evidence says the app is wrong, do not patch app code and do not
bend the flow around it. Instead:

1. Keep the flow asserting the correct behaviour, and leave it out of the
   suite (no soak, no `suite` tag).
2. Log a Bug ticket with `scent-ticket`, including the flow path, the failing
   step, the screenshot or hierarchy evidence, and a proposed `fix/` branch.
3. Report it to the user. A human decides whether the app changes.

## Soak and review-ready

A flow is only review-ready once `./scripts/e2e-soak.sh <flow>` passes 5/5.
The script tags it `suite` itself, and 4/5 means flaky, not nearly done. If
soak fails, go back to the loop: flakiness is a flow bug until the evidence
shows otherwise.

A review-ready PR contains:
- the flow file (and any new subflow or script),
- the output of the final `maestro test` run and of the soak (pass rate),
- one line per flow stating what it proves.

## How this sits with the other skills

- **`scent-dev-loop`** gates Kotlin changes. E2E is not one of its gates:
  flows need a device and CI has none yet (ADR-0002 defers CI). A PR that only
  touches `.maestro/` still has to clear the pre-push gate (`gates.sh`), which
  it will, since no Kotlin changed.
- **`scent-tdd`** owns unit and Compose UI tests. If a behaviour can be pinned
  below the UI, it belongs there, not in a flow (ADR-0002 layer ownership).
- **`scent-ticket`** logs the real bugs and app gaps this loop is not allowed
  to fix.
