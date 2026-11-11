---
name: scent-pre-merge
description: |
  Run the local pre-merge device check (Maestro flows + Android/iOS accessibility trees) on a
  Scent PR and act on the report. Use for "pre-merge check", "check this PR on devices", "run
  a11y before merge", or a PR number. Not the everyday loop (scent-dev-loop), which never boots
  a device, and not for writing E2E journeys (scent-e2e).
keywords: [scent, a11y, accessibility, pre-merge, maestro, axe, talkback, voiceover, ios, android]
---

# Scent Pre-Merge Device Check

The dev loop proves the Compose semantics tree is right on the JVM. This check proves those
semantics reach the real runtimes: TalkBack's `AccessibilityNodeInfo` tree on an emulator, and
VoiceOver's tree through Compose Multiplatform's iOS bridge on a simulator, at large text. It runs
locally, before merge, while the diff is being reviewed. Tiers and what each catches:
`docs/testing-a11y.md`.

Neither AXe nor Maestro runs TalkBack or VoiceOver. They read the tree a screen reader would
consume, and `scripts/a11y_check.py` judges it with fixed rules. **Never judge a raw dump by eye**;
read the report.

## 1. Get the branch

- A PR number: `gh pr checkout <n>`. Otherwise use the current branch.
- `git status` must be clean. Uncommitted changes are part of the diff the script scopes from, so
  a stray edit changes what runs. Commit or set it aside first.
- `git fetch origin` so `origin/main` is current.

## 2. Run it

```bash
scripts/pre-merge-check.sh            # --platform auto --flows changed
```

It boots what it needs (or reuses a running emulator or simulator), starts or reuses the E2E
backend through `scripts/e2e-up.sh`, builds both apps in parallel, runs the per-screen flows in
`.maestro/a11y/`, captures each tree and writes `build/pre-merge/report.md`. A cold run takes
several minutes. Run it once in the background and wait for it to finish; don't poll the log.

A server-only diff exits 0 straight away with the reason. That's a pass with nothing to verify on
devices; say so in the verdict.

## 3. Read the report

`build/pre-merge/report.md` has a table (platform × screen × flow, retried, tree, result) and, per
failing screen, each violation with rule, label, bounds and detail. `report.json` has the same.

Sort every row into one of two kinds **before touching code**:

| Result | Kind | Action |
|---|---|---|
| `infra-fail`: device didn't boot, build failed, flow failed twice, tree `ax-not-ready` | Infrastructure | Fix the environment or the flow (see `scent-e2e`). **Never change app code for these.** |
| `fail` with rule violations | Real a11y problem | Fix in shared Compose code (step 4) |

Flow failures follow `scent-e2e`'s rules: fix the flow, never the app, and report real bugs. A row
marked "retried" that then passed is a flake signal; mention it in the verdict.

Rules:

| Rule | Means | Usual cause |
|---|---|---|
| A11Y-001 | An actionable node has no label, on its own or merged from a child | Icon-only control with no `accessibleLabel`/`contentDescription`, or a control squeezed until its labelled child is laid out at zero size |
| A11Y-002 | Tap target under 48 dp (Android) / 44 pt (iOS) | Missing `minimumInteractiveComponentSize`, a fixed `height` |
| A11Y-003 | A toggle exposes no checked/selected state | `clickable` where `accessibleToggle` belongs, or the iOS bridge dropping `toggleable` state |
| A11Y-004 | A required label from `.maestro/a11y/expectations/<screen>.yaml` is missing | A label changed or was cleared by a `clearedDescription` higher up |
| A11Y-005 | iOS: the screen collapsed into one node | A `mergedGroup`/`clearAndSetSemantics` wrapping the screen, or the known CMP bridge bug |

A deliberate small target goes in that screen's `tap_target_allowlist`, with a comment saying why.
If an expectation itself is wrong (copy changed on purpose), update the expectations file in the
same PR and say so.

## 4. Fix in shared code, test first

For each real violation:

1. Trace it to the stateless `XContent` or the `ui/components` composable that renders the node.
   The label and bounds in the report locate it.
2. Fix it in `composeApp/src/commonMain` with the `ui/accessibility` modifiers (`accessibleLabel`,
   `accessibleClickable`, `mergedGroup`, `clearedDescription`, `accessibleToggle`,
   `accessibleState`, …), following ADS-STE100's merge-vs-clear rules. **Never** add an
   `iosMain`/`androidMain` override for accessibility. If the fix seems to need one, stop and say
   so: it's a bridge bug to report, not to patch around.
3. Per `scent-tdd`, write the failing Compose UI test first, at 2× font scale, asserting the
   behaviour the device check caught (label present, toggle state, ≥ 48 dp), then the fix.
4. Run the `scent-dev-loop` gates.

## 5. Re-run only what failed

```bash
scripts/pre-merge-check.sh --platform ios --screens marketplace-filters
```

`--screens` takes comma-separated names. Add `--keep-devices` while iterating, so the next run
skips the boot.

## 6. Xcode plugin (optional, never in the script)

`scripts/pre-merge-check.sh` is plain shell (`xcodebuild`, `simctl`, `axe`, `maestro`) and must
stay that way so it runs the same with no agent attached. The Xcode MCP tools (plugin
`xcode@claude-plugins-official`, bridged by `xcrun mcpbridge`) are an extra for the agent only.
Xcode must be running with Settings > Intelligence > Model Context Protocol switched on. Check what
the session exposes before relying on any of these. Open the project first with
`XcodeOpenWorkspace` (path `iosApp/iosApp.xcodeproj`, absolute) and pass the returned
`workspaceIdentifier` to later calls; close it with `XcodeCloseWorkspace` when done.

- **iOS build failure.** If available, call `BuildProject`, then `GetBuildLog` (severity `error`).
  Both return each error with `filePath` and line, and `GetBuildLog` names the build task, so a
  Kotlin/Native error shows up under "Compile Kotlin Framework" pointing at the `.kt` file
  (verified with a planted `iosMain` type error). Otherwise the script already prints the errors
  on `[ios]` lines; the full log is
  `build/pre-merge/ios-build.log` (`grep -nE ': error:|^error:|^e: '`). Kotlin/Native errors
  surface inside the "Compile Kotlin Framework" build phase as
  `<path>/X.kt:LINE:COL: error: …` followed by `error: Compilation finished with errors`; fix
  that Kotlin source, not Swift. Confirm with `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`.
- **XCUITest accessibility audit.** If an XCUITest target with `performAccessibilityAudit()`
  exists (`chore/ios-xcuitest-a11y-audit`), `GetTestList` lists it and `RunSomeTests` runs it:
  offer to run it for the changed screens, excluding `.dynamicType` and `.textClipped` as the
  nightly suite does. Today `GetTestList` returns 0 tests: say so and skip; don't build it here.
- **Real iPhone.** `XcodeListRunDestinations` shows a connected iPhone under "Devices". If one is
  there, offer to reinstall on it and repeat the iOS capture. Opt-in, never required to pass.
  Driving it (`DeviceInteractionStartWorkspaceSession`, `DeviceInteractionSynthesize`, which
  captures a screenshot and UI hierarchy) needs iOS 27+ on the device or simulator; on older
  runtimes the session refuses, so fall back to AXe on the simulator.
- **Apple docs.** If available, `DocumentationSearch` for how a UIKit accessibility trait or value
  behaves when an iOS finding depends on how the bridge maps Compose semantics onto UIKit.

Ignore the plugin's SwiftUI previews: Scent's screens are Compose, so a preview won't render
`XContent`.

## 7. Verdict

Finish with:

- **Ready to merge / not ready.**
- Violations found, and for each: fixed (with the test that now guards it) or left open, and why.
- Infrastructure problems, kept separate from violations.
- What wasn't verified: a platform skipped (e.g. iOS, because no shared UI changed), a screen with
  no flow yet, the Xcode plugin being unavailable.
- What this check **does not cover**: focus and reading order, announcement wording, contrast on
  iOS, live-region timing, and real TalkBack/VoiceOver behaviour. Those stay with the nightly or
  release tier and the manual TalkBack/VoiceOver pass (see `docs/testing-a11y.md`).

## Maintaining the check

- New screen: add `.maestro/a11y/<screen>.yaml` (tag `a11y`, select by label only, end settled on
  the screen), `expectations/<screen>.yaml`, and its source globs in `screens.yaml`. Lint with
  `scripts/e2e-lint.sh .maestro/a11y`.
- Checker change: test first in `scripts/tests/`, then run
  `uv run --with pytest --with pyyaml pytest scripts/tests` and `shellcheck -x scripts/pre-merge-check.sh`.
