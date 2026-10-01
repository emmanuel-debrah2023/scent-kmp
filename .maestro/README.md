# Maestro flows

End-to-end flows for the Android app. Install and one-off runs are covered in
the root `README.md`; this file is the rulebook every flow is held to.
`scripts/e2e-lint.sh` enforces the mechanical rules, and review covers the rest.

The rules follow Shopify's mobile E2E rebuild
([shopify.engineering/mobile-e2e-testing](https://shopify.engineering/mobile-e2e-testing)):
a small command surface, an assertion after every action, escape hatches that
must be justified, and a soak gate before a flow joins the suite.

## Layout

| Path | Holds |
|---|---|
| `flows/` | Standalone flows. `config.yaml` picks up everything here |
| `subflows/` | Reusable steps pulled in with `runFlow`, never run on their own by the suite |
| `scripts/` | JavaScript used by `runScript` (test data setup) |
| `.debug-output/` | Screenshots and hierarchy dumps from failed soak runs (gitignored) |

## Rules

### 1. Assert after every action

Each `tapOn`, `doubleTapOn`, `longPressOn`, `inputText`, `swipe` or `scroll`
must be followed by `assertVisible`, `assertNotVisible` or `extendedWaitUntil`
before the next action. That includes the last step of a flow. An action
whose outcome is never checked either passes when it did nothing, or fails one
step later with a misleading error.

### 2. Refute, then assert, on key transitions

When a step moves between screens (sign-in, submit, navigation), first prove
the old screen is gone and then prove the new one arrived:

```yaml
- tapOn: "Sign in"
- extendedWaitUntil:
    notVisible: "Welcome back"
    timeout: 15000
- assertNotVisible: "Welcome back"
- assertVisible: "Home"
```

Asserting only the new screen can pass against an overlay that sits on top of
a screen that never went away.

### 3. Select by what the user sees

Select by visible text or accessibility label (`contentDescription`). That
couples flows to the same thing users and screen readers depend on, and it
stays stable when layouts change.

Two traps, both hit while writing `sign-in-through-ui.yaml`:

- **A visible label is not its input.** Tapping `"Password"` taps the label
  text, not the field, and `assertVisible: "Password"` passes whether or not
  the field got focus. Assert the outcome instead: `focused: true` after
  tapping an input, and the exact field contents after typing.
- **Relative selectors need both ends.** A bare `below: "Password"` matches
  anything under the label, including the on-screen keyboard. Bound it
  (`below: "Password"` plus `above: "Sign in"`) and call `hideKeyboard`
  before tapping the next input.

Relative selectors are a stopgap for inputs with no label of their own. The
real fix is an accessible label in the app, which also fixes screen readers.

### 4. Escape hatches need a reason

These are allowed only with a trailing `# UNSAFE: <reason>` comment on the same line:

| Construct | Why it's unsafe |
|---|---|
| `id:` selectors | Couples the flow to implementation detail invisible to users |
| `optional: true` | Hides a step that may silently stop happening |
| Timeout overrides, including `extendedWaitUntil` above 15000 ms | Masks slowness that is often the real bug |

```yaml
- tapOn:
    id: "listing_card_0" # UNSAFE: cards have no text until images load
```

Coordinate taps (`point:`) are banned outright. There is no justification
that survives a different screen size.

### 5. Soak before the suite

A new flow must pass `./scripts/e2e-soak.sh <flow>`, which runs it 5 times
from a cleared app state. Only a 5/5 result earns the `suite` tag, and the
script adds that tag itself. 4/5 is a flaky flow, not a nearly-passing one.
Failed runs keep their debug output under `.debug-output/<flow>/run-N/`.

Run the soaked suite with:

```bash
maestro test .maestro --include-tags suite
```

## Starting signed in

Only the auth flow drives the login and register screens. Every other flow
starts signed in through the gray-box subflow (ADR-0002):

```yaml
- runFlow: ../subflows/start-authenticated.yaml
```

Its `onFlowStart` script calls the dev-only `POST /api/v1/dev/seed-user` to
make sure the E2E account exists, then logs in through `/api/v1/auth/login`
for a real JWT. The app is launched with that token as the `e2eToken` launch
argument. A debug-only hook writes it to token storage before the auth gate,
and the normal `/auth/me` check then runs exactly as on a relaunch, so an
invalid token lands on the login screen rather than faking a session.
`flows/invalid-token-falls-back-to-login.yaml` guards that behaviour.

The backend must be running on `localhost:8080` with dev routes mounted.
One command brings up Postgres, the server and the seeded data:

```bash
./scripts/e2e-up.sh          # reuses anything already running
./scripts/e2e-up.sh --down   # stops a server it started
```

Override the account with `-e E2E_EMAIL=... -e E2E_PASSWORD=...` or the
backend with `-e E2E_API_URL=...`.

`subflows/sign-in-through-ui.yaml` seeds the same account but signs in through
the login screen. It is a fallback for platforms without the launch argument
hook (iOS today), not a default.
