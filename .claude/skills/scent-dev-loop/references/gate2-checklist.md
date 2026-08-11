# Gate 2 — ADS-STE100 self-check

Use this only for small, single-file changes where spawning
`ads-ste100-auditor` isn't worth it. For anything touching more than two files,
or any change to a repository, mapper, or navigation, use the subagent — it
reads the full guideline and you don't.

The authoritative source is `docs/architecture-guidelines.md`. This is a
compression of it, and where they disagree, the guideline wins.

---

## Null-safety boundary

- [ ] DTO fields all nullable, all with `= null`
- [ ] `@SerialName` on every snake_case wire field
- [ ] Wire enums typed as `String?`, converted via `fromString()` with a fallback
- [ ] Domain model non-null with defaults (`emptyList()`, `""`, `0`, `false`)
- [ ] Nullable in the domain model only where absence is semantic
- [ ] Mapping lives in `data/mapper/` — not the repository, not the ViewModel
- [ ] `toDomain()` returns `Either<AppError, T>`, validating required fields first
- [ ] `ParseError` names the offending field
- [ ] `toDomainList()` drops invalid entries rather than failing the page
- [ ] No `!!` anywhere (also blocked at write time by hook)

## Error handling

- [ ] Every repository method and use case returns `Result<T>`
- [ ] Built with `asRight()` / `asLeft()`
- [ ] Only sealed `AppError` subclasses; new failure mode = new subclass
- [ ] Use cases validate params before hitting the repository
- [ ] HTTP status codes mapped inside the repository, never leaked upward
- [ ] `try/catch` only at the platform edge; specific exceptions first;
      generic `Exception` re-wrapped as `AppError.Unknown(cause = e)`
- [ ] No `Left` silently discarded

## Dependency injection

- [ ] Constructor injection only; no `get<T>()` / `by inject()` in class bodies
- [ ] New class registered in the correct module (`networkModule`,
      `databaseModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`)
- [ ] Correct scope: `single` for repos/clients/DAOs, `factory` for use cases,
      `viewModel` for ViewModels
- [ ] Bound to the interface, not the implementation

## ViewModels

- [ ] One `StateFlow<UiState<T>>`, exposed via `asStateFlow()`
- [ ] Extends `BaseViewModel`; one-shot errors via `SharedFlow<AppError>`
- [ ] Results consumed via `handleResult(onSuccess =, onError =)`
- [ ] No business logic, no platform types, no `runBlocking` / `GlobalScope`

## Navigation

- [ ] Route added to the correct **per-tab** sealed interface
      (`HomeRoute` / `MarketplaceRoute` / `SearchRoute` / `ProfileRoute`)
- [ ] Shared destinations (`FragranceDetail`, `ListingDetail`, `UserProfile`)
      still duplicated per tab — **not** unified into a shared `DetailRoute`
- [ ] Navigation into a shared destination uses the *current tab's* copy
- [ ] Callbacks passed down; no `NavigationState` in leaf composables
- [ ] Back-press policy untouched (pop → Home → exit)

## Design system

- [ ] No hex literals, no `Color(0xFF...)`, no inline `TextStyle`
- [ ] M3 token names only (`MaterialTheme.colorScheme.*`, `.typography.*`)
- [ ] Gold used as fill/shape on cream — never as text or thin outline
- [ ] Buttons flat at every state; cards keep the warm-tinted shadow
- [ ] Inputs bottom-border only, `outlineVariant` → `primary` on focus
- [ ] 52px touch targets; 12px radius on containers and buttons
- [ ] Existing components in `ui/components/` reused rather than re-rolled

## Component API (AndroidX guidelines, app tier)

- [ ] Parameter order: required → `modifier: Modifier = Modifier` → optional →
      trailing `content`
- [ ] Exactly one modifier param, named `modifier`, defaulting to `Modifier`
- [ ] Modifier applied **first** in the chain on the root-most layout
      (`Box(modifier.padding(…))`, not `.then(modifier)`)
- [ ] No parameter duplicating what a modifier already does
- [ ] No `MutableState<T>` or `State<T>` parameters — value in, callback out
- [ ] Leaf/mid-level components stateless; only screens touch a ViewModel
- [ ] Slots (`@Composable` lambdas) rather than `String` / `ImageBitmap` params,
      scoped (`RowScope`/`ColumnScope`) where layout is implied
- [ ] Variants are separate composables, not a `style` parameter or config class
- [ ] Defaults public and meaningful; `null` means absence, not "use default"
- [ ] `MaterialTheme.*` read in default expressions, not in the implementation
- [ ] Leaf components take a required nullable `contentDescription`
- [ ] Interaction via `Modifier.clickable`/`toggleable`/`selectable` rather than
      raw `pointerInput` plus hand-rolled semantics
- [ ] `@Preview` present; initial render not gated on `LaunchedEffect`

## Tests

- [ ] ViewModels, repositories, use cases, mappers, validators covered
- [ ] Fakes, not mocks
- [ ] Every `Either.Left` branch has a test case
- [ ] Arrange-Act-Assert, backtick names describing behaviour
- [ ] `runTest` for coroutines

## KMP

- [ ] `commonMain` changes compile on every consuming target
- [ ] No platform types leaked into `commonMain`
- [ ] `expect`/`actual` pairs complete for both Android and iOS
