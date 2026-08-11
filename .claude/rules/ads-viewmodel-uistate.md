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
booleans alongside it.

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
