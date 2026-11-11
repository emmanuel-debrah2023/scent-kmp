# Accessibility testing

How Scent checks that its Compose semantics reach TalkBack and VoiceOver. The rules for writing
those semantics live in ADS-STE100 (`architecture-guidelines.md`, "Accessibility"). This page covers
how they are verified, in three tiers, each catching what the cheaper tier before it can't.

## Tiers

| Tier | When | Tooling | Catches |
|---|---|---|---|
| Dev loop | Every change | JVM Compose UI tests of the stateless `XContent`, at 2× font scale | Missing labels, unreachable CTAs, clipping, in the semantics tree |
| Pre-merge | Locally before merge, while reviewing | `scripts/pre-merge-check.sh`: Maestro flows, then the Android tree (uiautomator) and the iOS tree (AXe), judged by `scripts/a11y_check.py` | Labels actually reaching TalkBack and VoiceOver on the real runtimes, tap targets, toggle state, the iOS bridge collapsing a screen |
| Nightly / release | Nightly CI, each release | Full Maestro a11y suite, XCUITest `performAccessibilityAudit()`, manual TalkBack and VoiceOver pass | Focus and reading order, announcement wording, contrast, everything else |

**Not built yet.** The 2× font scale JVM helpers, the nightly workflow, the XCUITest audit target
and the full a11y suite don't exist today. Until they do, the pre-merge check and the manual
release pass are the only device-level coverage. Update this table as each one lands.

## Why a pre-merge tier

The dev loop never boots a device, which keeps it fast. But a JVM test checks the semantics tree,
not whether a platform exposes it. On iOS, Compose semantics reach VoiceOver through Compose
Multiplatform's accessibility bridge, and Dynamic Type reaches Compose as `fontScale`. The bridge
has had real bugs, such as whole screens collapsing into one node. Only a running app shows these.

The check runs locally because a cold boot, two builds and the flows take minutes: too slow for
every change, fine while reading a diff. Each platform boots once and runs its flows and its tree
captures on the same device, and the two platforms run in parallel.

## What the pre-merge check does

1. Scopes from `git diff` against `origin/main`. Server-only changes skip devices. Changed screens
   map to flows through `.maestro/a11y/screens.yaml`. When the mapping is unclear, every screen
   runs.
2. Boots an Android emulator at 2× font scale and an iPhone simulator at
   `accessibility-large` Dynamic Type. TalkBack and VoiceOver stay off.
3. Runs `.maestro/a11y/<screen>.yaml` for each screen. Each flow ends settled on its screen. A
   failed flow is retried once and marked as retried.
4. Dumps the tree once two consecutive dumps match, then checks it against these rules:

   | Rule | Check |
   |---|---|
   | A11Y-001 | Every actionable node has a label, its own or a merged child's |
   | A11Y-002 | Tap targets are at least 48×48 dp (Android) or 44×44 pt (iOS) |
   | A11Y-003 | Toggles expose a checked or selected state |
   | A11Y-004 | The labels in `.maestro/a11y/expectations/<screen>.yaml` are present |
   | A11Y-005 | iOS only: the screen isn't collapsed into one node |

5. Writes `build/pre-merge/report.md`, keeping infrastructure failures (no boot, a flow failing
   twice, an empty iOS tree) apart from accessibility violations.

The `scent-pre-merge` skill drives this and fixes what it finds.

## Choices worth knowing

- **AXe for iOS.** `axe describe-ui` reads the simulator's accessibility tree with labels, values,
  types and frames in points. Maestro's iOS hierarchy is normalised and loses some of that.
  iOS 26 simulators start with `AccessibilityEnabled` and `ApplicationAccessibilityEnabled` off,
  which makes the tree come back empty, so the script writes both to
  `com.apple.Accessibility` before launching.
- **uiautomator for Android, not Maestro's hierarchy.** Maestro's `hierarchy` output has `checked`
  but no `checkable`, so an unchecked toggle looks like plain text and A11Y-003 couldn't work.
  `uiautomator dump` has both and is the same `AccessibilityNodeInfo` tree TalkBack reads.
- **The checker is Python, not a `.main.kts`.** It parses JSON and XML and needs fixture tests;
  Python's standard library and pytest do that without touching the Gradle build. `uv` resolves
  its one dependency (PyYAML) from the script's own header.
- **iOS signs in at default text size.** iOS has no launch-argument hook for a seeded session, so
  it signs in through the login screen once and later flows reuse the session. At accessibility
  sizes the keyboard covers SIGN IN and can't be dismissed, so sign-in happens before Dynamic Type
  goes up.
- **Fixes go in shared Compose code**, using the `ui/accessibility` modifiers. Never in `iosMain`
  or `androidMain`.

## Limits

The pre-merge check reads trees; it doesn't listen to a screen reader. It can't judge focus or
reading order, announcement wording, contrast on iOS, or live region timing. Those stay with the
nightly and release tier.
