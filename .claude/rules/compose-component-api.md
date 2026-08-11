---
paths:
  - "composeApp/src/**/ui/**"
  - "composeApp/src/**/*Screen.kt"
  - "composeApp/src/**/*Composable.kt"
  - "**/ui/components/**"
---

# Compose component API

Adapted from AndroidX's component API guidelines. That doc tiers its rules by
audience; Scent is **app development**, so parameter rules are binding, naming
and structure are guidance, and the API-evolution section (deprecation overloads
for binary compatibility) is skipped entirely — internal app composables have no
binary compatibility surface.

Paired with `ads-design-system.md`, which covers what things look like. This
covers what the function signature looks like.

## Parameter order — not negotiable

1. Required parameters (data first, then metadata)
2. `modifier: Modifier = Modifier` — exactly one, first optional
3. Remaining optional parameters
4. Trailing `@Composable` lambda, usually `content`

```kotlin
@Composable
fun FragranceCard(
    fragrance: Fragrance,              // required data
    onOpen: () -> Unit,                // required behaviour
    modifier: Modifier = Modifier,     // first optional
    showPrice: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
)
```

## The modifier parameter

Every composable emitting UI takes one. Type `Modifier`, named `modifier`,
default `Modifier`, and **applied first in the chain on the root-most layout**:

```kotlin
Box(modifier.padding(16.dp))          // correct
Box(Modifier.padding(16.dp).then(modifier))   // wrong — caller's modifier loses
```

One modifier per component. Needing `rowModifier` and `iconModifier` means the
component should be split or should expose slots.

Don't add a parameter for something a modifier already does — no `onClick` on a
non-interactive component (`Modifier.clickable`), no `clipToCircle`
(`Modifier.clip`). Parameters configure behaviour the component genuinely owns.

## State

- **Hoist it.** Leaf and mid-level components are stateless: value in, event
  callback out. Only screen-level composables read a ViewModel.
- **Never take `MutableState<T>`** as a parameter — it splits ownership. Take
  `value: T` plus `onValueChange: (T) -> Unit`.
- **Never take `State<T>`** — it narrows what callers can pass. Take `T`
  directly, or `() -> T` when the read should be deferred to draw/layout.
- This mirrors `ads-viewmodel-uistate.md` (never expose `MutableStateFlow`) and
  `ads-navigation.md` (callbacks down, never navigation state down). Same
  principle at three layers.

## Slots over data parameters

Take `@Composable` lambdas, not `String` and `ImageBitmap`:

```kotlin
// don't — locks callers out of AnnotatedString, custom text components,
// custom arrangement, and breeds overloads
@Composable fun ListingRow(title: String, icon: ImageBitmap?)

// do
@Composable fun ListingRow(title: @Composable () -> Unit, icon: @Composable () -> Unit)
```

Give the slot a scope when it implies a layout (`RowScope`, `ColumnScope`) so
callers know how multiple children will arrange. The social sign-in buttons in
`design.md` are the obvious case: an icon slot, not an `ImageVector` parameter.

Avoid DSL-scoped slots (`content: MyScope.() -> Unit`); plain composable lambdas
are enough outside genuinely lazy APIs.

If a component juggles several slots, consider also offering a single `content`
overload so callers can take over the arrangement.

## Naming

- **No `Scent` prefix.** These live in your own package; `Button` and `Card`
  read as first-class. Prefix only to disambiguate from an M3 import in the same
  file.
- **Variants get separate composables, not a style parameter.** `design.md`
  defines primary and secondary buttons — that's `PrimaryButton()` and
  `SecondaryButton()`, never `Button(style = ButtonStyle.Primary)`. A grab-bag
  `ComponentStyle` / `ComponentConfiguration` class is the anti-pattern here.
- The most-used variant may drop its prefix.

## Defaults

Default expressions must be **public and meaningful** so callers wrapping the
component can reuse them. Never `null` as a "resolve internally" marker —
nullable means genuine absence (a null `contentDescription` is decorative;
`""` is not the same thing).

More than two or three defaults → a public `FooDefaults` object. Reading
`MaterialTheme.colorScheme.*` **in a default expression** is correct; reading it
buried in the implementation isn't, because callers can't override it.

## Before adding a component

Write the call site out of existing building blocks first. If it reads fine as a
`Column` with a `Text` and a `Card`, it doesn't need to be a component — every
component is API surface someone has to learn and you have to maintain.

Prefer a `Modifier` extension when the behaviour applies to any single
composable; a component only when it has distinct UI or must change the
hierarchy.

## Accessibility

Leaf components take a required `contentDescription: String?` — required so the
decision is conscious, nullable so "decorative" is expressible.

Prefer foundation primitives (`Modifier.clickable`, `Modifier.toggleable`,
`Modifier.selectable`), which bring semantics and merging for free, over
`Modifier.pointerInput` plus hand-written semantics. Interactive targets stay at
the 52px `button-height` token.

## Previews and testing

Every reusable component gets a `@Preview`. Stateless components are trivially
previewable and screenshot-testable in each state — a component that needs a
ViewModel to render is neither.

Don't gate initial render on `LaunchedEffect` or async work; previews render one
non-interactive frame. (Screen-level `LaunchedEffect` for collecting the error
`SharedFlow` is fine — that's not initial state.)
