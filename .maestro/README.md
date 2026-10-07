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

Inputs are reached by their label (`tapOn: "Password"`): auth text fields
carry their label for accessibility. Compose exposes that label as a child
node of the field, so assert focus on the field that contains it:

```yaml
- tapOn: "Password"
- assertVisible:
    containsChild: "Password"
    focused: true
```

Three traps, all hit while writing the sign-in and auth flows:

- **Prove the tap landed.** `assertVisible: "Password"` passes whether or not
  the field got focus. Assert the outcome instead: `focused: true` after
  tapping an input, and the exact field contents after typing. Before the
  label fix, this is what caught taps hitting the visible label text.
- **Relative selectors need both ends.** A bare `below: "Password"` matches
  anything under the label, including the on-screen keyboard. Bound it
  (`below: "Password"` plus `above: "Sign in"`) and call `hideKeyboard`
  before tapping the next input.
- **Text selectors are full-match regexes.** `"Already have an account?"`
  never matches the node `"Already have an account? "`: the `?` makes the `t`
  optional and the trailing space is missing. Pick a plain label nearby, or
  escape the special characters and match the whole text.

Relative selectors are a stopgap for elements with no label of their own. The
real fix is an accessible label in the app, which also fixes screen readers;
that is how the auth inputs were fixed.

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

Only the auth flow, `flows/auth-register-login-logout.yaml`, drives the login
and register screens: it registers a fresh account, checks the session survives
a relaunch, logs out, checks that survives a relaunch too, and logs in with the
seeded account. Every other flow starts signed in through the gray-box subflow
(ADR-0002):

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

To open on the screen a flow is about instead of Home, pass `E2E_ROUTE`. It
reaches the app as the `e2eRoute` launch argument:

```yaml
- runFlow:
    file: ../subflows/start-authenticated.yaml
    env:
      E2E_ROUTE: marketplace
```

Accepted routes are `home`, `search`, `marketplace`, `profile` and
`profile/create-listing` (the list lives in `E2eLaunchArguments.kt`). An
unknown route is logged and the app starts on Home, so assert the target
screen straight after the subflow rather than trusting the route.

To skip the system photo picker, pass `E2E_FAKE_IMAGES: 'true'` the same way. It
reaches the app as the `e2eFakeImages` launch argument, and Add photos then
returns a bundled 64x64 PNG without opening the picker, one photo per tap. It
only works in debug builds. Uploads need the server started by `e2e-up.sh`,
which sets `IMAGE_PROVIDER=fake`; an `.env` with a different `IMAGE_PROVIDER`
overrides it.

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
hook (iOS today), not a default. The login steps themselves live in
`subflows/log-in-through-ui.yaml`, which starts on the login screen and takes
the account as `env`:

```yaml
- runFlow:
    file: ../subflows/log-in-through-ui.yaml
    env:
      LOGIN_EMAIL: ${output.e2eEmail}
      LOGIN_PASSWORD: ${output.e2ePassword}
```

## Resetting server state

`clearState` resets the app, not the server. A flow that creates server data,
such as publishing a listing, leaves it behind, and over many runs that data
fills the newest-first marketplace feed and the profile Listings tab. Flows
that create server data should reset it.

`scripts/reset-e2e-listings.js` does this for listings. It calls the dev-only
`POST /api/v1/dev/reset-listings` route with the E2E email, which deletes that
account's listings and any photos nothing else uses, and sets
`output.e2eListingsRemoved`. The route only accepts the E2E accounts
(`scent_e2e` and the `e2e_*` registration accounts) and answers 403 for any
other user, so it cannot clear real data. It answers 200 with zero removed when
there is nothing to clear, which makes it safe to run repeatedly.

A flow that creates server data should run the script from both `onFlowStart`
and `onFlowComplete`. Maestro runs `onFlowComplete` even after a failed flow
(checked with a throwaway flow whose assertion fails), so a failure does not
leave listings behind, and `onFlowStart` clears anything left by a run that was
killed. If the account does not exist yet when `onFlowStart` runs, the
route answers 404 and the script carries on with zero removed. A 404 with an
empty body means the running server predates the route, so restart it with
`./scripts/e2e-up.sh --down && ./scripts/e2e-up.sh`.
