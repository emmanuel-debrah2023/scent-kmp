# ADR-0005: The root scaffold consumes the bar insets; each screen owns its IME padding

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Emmanuel
- **Ticket:** audit finding N12 (follow-up branch `fix/form-ime-insets`); found on the device pass for the Ask tab ticket ("Verify the Ask tab end to end", PR #152)

## Context

The app draws edge to edge: `MainActivity` calls `enableEdgeToEdge()`, so the system bars, the display
cutout and the keyboard all reach Compose as window insets, and nothing pads for them unless Compose does.

`BlendedScaffold` is the root scaffold. It overlays the header and the bottom nav and hands its content
the measured heights as `innerPadding`, the way the M3 `Scaffold` does. Like the M3 `Scaffold`, it never
marks those insets as consumed. `MainGraph` applied the padding with `Modifier.padding(innerPadding)` and
stopped there.

`imePadding()` was used in exactly one place, the Ask tab's form root (`AskContent`). Every other form
(Create Listing, Edit Listing, Edit Profile, Login) has no IME handling at all, and the manifest does not
set `windowSoftInputMode`. On the Ask tab the unconsumed padding showed up as a bug: `imePadding()` added
the navigation-bar and bottom-bar height a second time, leaving a dead band of about 106dp above the
keyboard. At 200% font with a four-line query, that band pushed the Ask button off screen.

## Decision

Each inset has one owner:

- The root scaffold owns the system-bar and display-cutout insets. Screens never pad for the status bar
  or navigation bar themselves.
- The content root applies the scaffold's padding and consumes it in the same place:
  `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)`. In Scent that is `MainGraph`, once
  for every tab. Full-bleed screens that use `innerPadding` as `contentPadding` (Home) consume it first if
  they ever take text input.
- The screen owns IME padding, at its form root or on a pinned action bar. The scaffold does not, because
  not every screen should resize when the keyboard opens.
- Nested scaffolds (sheets, sub-flows) pass `contentWindowInsets = WindowInsets(0)`.
- The Android activity sets `android:windowSoftInputMode="adjustResize"` explicitly.
- Multiline fields use a `BringIntoViewRequester` so the cursor stays above the keyboard.

The rules live in ADS-STE100 under "Window Insets Ownership".

## Consequences

- `imePadding()` is safe on any screen under `MainGraph`, because the bar insets are consumed once,
  above it. The Ask tab fix (PR #152) is the first use.
- Each form has to add its own `imePadding()`. Create Listing, Edit Listing, Edit Profile (with a
  `BringIntoViewRequester` on Bio) and Login still need it, as does the `adjustResize` manifest entry.
  That work is `fix/form-ime-insets`.
- Screens that should not move with the keyboard (the video feed) stay put without opting out of anything.
- A screen that pads for the status bar or navigation bar itself breaks the rule. It shows up as double
  padding, so review catches it.
- iOS gets the same insets through Compose Multiplatform, but keyboard behaviour there has not been
  checked on a simulator yet. Treat it as verify on device until it has.
