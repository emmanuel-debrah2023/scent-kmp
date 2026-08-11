---
paths:
  - "composeApp/src/**/navigation/**"
  - "**/AppNavigator.kt"
  - "**/NavigationState.kt"
  - "**/Routes.kt"
  - "**/Tab.kt"
---

# ADS-STE100 — Per-tab navigation

Four tabs (`Home`, `Marketplace`, `Search`, `Profile`), each owning an
**isolated back stack** via its own `NavigationState<Route>` instance.
`AppNavigator` holds one per tab and centralises the back-press policy.

**Shared destinations are duplicated on purpose.** `FragranceDetail`,
`ListingDetail`, and `UserProfile` appear in several per-tab route hierarchies.
Do **not** refactor them into a shared `DetailRoute` sealed interface — that
reintroduces "which tab does this belong to?" ambiguity and breaks the typed
per-tab state. Accept the verbosity. Revisit only if shared destinations come to
outnumber tab-specific ones, and raise it explicitly rather than doing it inline.

Navigate to the **current tab's own** copy:
```kotlin
// inside Search
onOpenFragrance = { id -> searchState.navigateTo(SearchRoute.FragranceDetail(id)) }
```

**Back-press policy** (do not modify without being asked):
1. Active tab can pop within its stack → pop.
2. Active tab at root and not Home → switch to Home.
3. Home at root → return `false`, host exits the app.

Re-tapping the already-active bottom-nav item pops that tab to root.

**Callbacks down, never state down.** Screens receive
`onNavigateToX: (String) -> Unit`. Never pass or inject a `NavigationState` into
a deep composable — a leaf component must not know navigation exists.

**No navigation libraries.** State-based navigation is the current approach
until official Compose Multiplatform Navigation stabilises. Do not add
Voyager/Decompose/androidx-navigation, and do not run two approaches
concurrently, without an explicit request. The per-tab shape is designed to map
onto nested graphs later; keep it mappable.
