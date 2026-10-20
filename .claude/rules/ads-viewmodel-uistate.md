---
paths:
  - "**/*ViewModel.kt"
  - "**/ui/base/**"
---

# ADS-STE100 — ViewModel and UiState

**State.** One `StateFlow<UiState<T>>` per screen, exposed read-only via
`asStateFlow()`, backed by a private `MutableStateFlow`. `UiState` is the shared
sealed class — `Idle`, `Loading`, `Success<T>`, `Error`. Do not invent a
per-screen state enum, and do not expose separate `isLoading` / `errorMessage`
booleans alongside it. The one exception is a multi-phase A2UI surface screen
(below).

**Exception: multi-phase A2UI surface screens.** A screen that drives a
server-composed A2UI surface through repeated round trips (the Ask tab, and any
later surface screen with the same shape) may expose one `StateFlow<XUiState>`
whose `phase` is a sealed interface, instead of `UiState<T>`. The reason is
specific: a failed follow-up round trip must leave the surface and the user's
edits on screen with a retry, and `UiState.Loading` / `UiState.Error` would
discard them. All of these hold, or the screen uses `UiState<T>`:
- one read-only `StateFlow`, one private `MutableStateFlow`, as above;
- `phase` is a sealed interface whose failure cases carry an `AppError`, and it
  encodes loading, so there is still no `isLoading` / `errorMessage` boolean. A
  flag the phase cannot express (e.g. "slow after 5 s") is allowed;
- failures live in the state so the screen can show them beside the retry; the
  inherited `SharedFlow` is not the only channel;
- results are still consumed with `handleResult`, and the reducer's
  `SurfaceState` stays private to the ViewModel or is resolved into the phase;
- a sub-part that is a plain load-then-show (no state to preserve) still uses
  `UiState<T>`;
- the ViewModel test asserts each phase transition, including failure with the
  prior surface and answers preserved.
The first user is `AskViewModel` (`AskUiState` / `AskPhase`).

**Errors.** Extend `BaseViewModel`. One-shot errors (snackbars, toasts) go out
through the inherited `SharedFlow<AppError>` via `handleError()`. Errors that
should replace the screen body set `UiState.Error(error)`. A failure that does
both sets the state *and* emits.

**Folding.** Consume repository/use-case results with the inherited
`handleResult(onSuccess =, onError =)` helper rather than a hand-rolled `when`
on `Either`.

**No business logic here.** Validation, mapping, filtering, sorting, and
combination of sources belong in a use case or repository. A ViewModel launches
a coroutine, calls one collaborator, and translates the result into state. If a
ViewModel is doing arithmetic on domain objects, that logic is in the wrong file.

**No Android/platform types in the shared surface** — no `Context`, no
`Resources`, no `android.net.Uri` in constructor params or state. User-facing
strings come from `AppError.message` or the composable, not from the ViewModel.

`viewModelScope.launch` for everything async. Never `runBlocking`, never
`GlobalScope`.
