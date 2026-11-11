# Scent Platform Guidelines

**Status:** Active · **Decided:** 2026-11-08 · **Checked against:** `main` @ `72925dd` · **Review:** when the Cupertino spike reports ([Spike: compose-cupertino adaptive components for iOS](https://app.notion.com/p/41211d6b186b81adb698d2480c7c023d), branch `chore/spike-compose-cupertino`)
**Lives at:** `docs/platform-guidelines.md`, linked from ADS-STE100 (`docs/architecture-guidelines.md`).

This is the rulebook for **how a Scent screen should behave on Android and iOS, and which component to use**. It sits between two other docs:

| Doc | Answers |
|---|---|
| `design.md` | How things *look*: tokens, type, shape, elevation |
| **`platform-guidelines.md` (this doc)** | Which *component* to use, which Material 3 and Apple HIG rules apply, what's shared vs adapted per platform, and where the brand deliberately departs from both |
| `architecture-guidelines.md` (ADS-STE100) | How the *code* is structured, including the `ui/accessibility` extensions |

**Every skill reads this doc instead of copying its rules.** `scent-platform-ux` designs and reviews against it, and `scent-ticket` and `scent-dev-loop` point to it. `kiro-android-review` and `mobile-ecosystem-watch` should too, but aren't hooked up yet. If a rule needs to change, change it here (and record why in section 11). Don't patch it inside a skill.

---

## 1. Decision: Compose Multiplatform + Material 3 everywhere (for now)

- Scent's UI is shared Compose Multiplatform. CMP draws its own UI and doesn't use UIKit/SwiftUI components, so iOS gets whatever we render. Nothing on iOS looks native unless we choose it.
- **Default:** Compose Material 3 components, themed by `ScentTheme` (the M3 token names in `design.md`), on **both** platforms.
- **iOS adaptation is limited to behaviour, not components** (section 2). Component swaps to Cupertino widgets wait for the spike.
- **🧪 spike candidates** (section 6) are the components most likely to feel foreign on iOS. They're the first ones `compose-cupertino` would replace if the spike recommends it. Until then they ship as Material.
- **Brand beats platform for look; platform beats brand for behaviour.** Scent's editorial look (cream surfaces, Playfair/DM Sans, forest green, warm card shadow, glass over video) is identical on both platforms. How things *behave* (back, gestures, safe areas, system settings) follows each platform.

---

## 2. Shared vs adapted

| Behaviour | Android | iOS | Status |
|---|---|---|---|
| Back navigation | `SystemBackHandler` (androidMain wraps the activity back callback) runs the ADS-STE100 policy: pop the tab stack → switch to Home. At Home root the handler is **disabled**, so the system does back-to-home | Same shared policy. `SystemBackHandler` (iosMain) maps to the native edge swipe | Share (implemented). **Open question:** under the shared policy an iOS edge swipe at a non-Home tab root switches to Home, which the HIG doesn't do (a swipe at a tab root does nothing). Decide in the spike or an ADR; the policy itself is "do not modify without being asked" (`ads-navigation.md`) |
| Back icon | `BackButton` (`ui/components/BackButton.kt`) uses `AutoMirrored.Outlined.ArrowBack` | Same arrow; the HIG uses a chevron | **Open.** If adopted, add an iOS chevron variant inside `BackButton` via expect/actual. Nothing exists for this today |
| Tab bar on pushed screens | Visible | Visible | Share. Hide it only for full-screen task flows (create post, checkout) and the video feed's immersive mode |
| Safe areas | Edge-to-edge with `WindowInsets.safeDrawing` | Notch / Dynamic Island / home indicator via the same insets | Share. Never hardcode top/bottom padding |
| Keyboard | `imePadding()`, `imeAction` | Same | Share |
| Text size | `sp` typography tokens follow font scale | Dynamic Type reaches Compose as `fontScale` | Share (enforced by scent-dev-loop) |
| Pressed vs hover | Pressed states only | Pressed states only | Share. Touch has no hover; `design.md`'s "hover" means pressed on mobile |
| Haptics | `LocalHapticFeedback` on like/save, long-press, destructive confirm | Same | Share. **Not adopted yet** (no usage in the repo); add when the first toggle is built |
| Glass / translucency | `GlassSurface` (**planned**: Haze and `GlassSurface` don't exist yet; see the Glass feed overlay and notes tickets), honours increase-contrast | Same, honours Reduce Transparency → solid cards | Share once built |
| Reduced motion | Shimmer, parallax and glass lighting off | Same (Reduce Motion) | Share |
| Scroll physics / overscroll | Platform default | Platform default | Share. Don't override |
| Pull to refresh | `PullToRefreshBox` | Same | Share. Not used yet |
| Components | Material 3 | Material 3 (🧪 candidates under review) | Share until the spike reports |

---

## 3. Material 3 rules we adopt

These apply on both platforms because both render Material components.

1. **Colour roles, not colours.** Every colour is an M3 role from `design.md` (`primary`, `on-primary`, `surface-container-*`, …), always used with its `on-` partner. No hex literals in UI code.
2. **Touch targets ≥ 48dp**, even when the visual element is smaller (icon buttons, chips, stars).
3. **Type comes from the type scale tokens in `sp`.** No one-off text styles.
4. **Navigation bar for 3–5 top-level destinations**, labels always shown. Scent has four. Tabs are destinations, never actions.
5. **One primary action per screen.** Other actions step down: outlined → text → icon.
6. **Dialogs are for decisions that block the user** (destructive confirm, unsaved changes). Never for information that a snackbar or inline message can carry.
7. **Snackbars:** one at a time, short copy, at most one action, never the only place critical information appears.
8. **Bottom sheets** for contextual actions and choices. Modal sheets get a visible drag handle and dismiss on scrim tap or swipe down.
9. **Edge-to-edge** on Android: content draws behind system bars, padded by insets. Apps targeting Android 15+ get it enforced anyway.
10. **Predictive back is never blocked**, except for an unsaved-changes confirm on task flows.
11. **Contrast:** text ≥ 4.5:1, large text and non-text UI (icons, borders, focus) ≥ 3:1, in every state.
12. **States are visible:** focused, pressed, selected, disabled and error each look different, and don't rely on colour alone.

## 4. Apple HIG rules we adopt

These apply on iOS even though the components are Material.

1. **Touch targets ≥ 44 × 44pt.** Satisfied by the 48dp rule.
2. **Back is a chevron plus edge swipe**, and swipe-back must work on every pushed screen. A pushed screen that can't be swiped back feels broken on iOS.
3. **Tab bar = top-level navigation only**, and it stays put while you move within a tab. Re-tapping the active tab pops to its root and then scrolls to top.
4. **Modality is for focused tasks.** Full-screen task flows (create post, checkout) have explicit Cancel and a primary Done/Post/Pay. Discarding unsaved work asks first.
5. **Alerts are rare.** Destructive actions are labelled with the verb ("Delete listing"), not "OK", and there's always a Cancel.
6. **No hamburger menus.** Everything top-level is in the tab bar; everything else is reachable from a tab.
7. **Respect system settings:** Dynamic Type, Bold Text, Reduce Motion, Reduce Transparency, Increase Contrast, Smart Invert.
8. **Liquid Glass belongs to the control layer.** HIG puts glass on navigation and controls (tab bars, toolbars, sheets), not on content. Scent's video overlay controls follow this; the fragrance notes cards are a deliberate deviation (section 5).
9. **Haptics mean something:** confirmation, toggles, errors. Never decoration.
10. **Search lives where users expect it.** Scent's dedicated Search tab matches iOS 26's search-in-tab-bar pattern.

---

## 5. Deliberate brand deviations

Each of these departs from Material 3, the HIG, or both **on purpose**. A review doesn't flag them as problems, but it does check the guard is met.

| Deviation | Departs from | Why | Guard (must still hold) |
|---|---|---|---|
| Text fields are bottom-border only, label above | M3 filled/outlined fields | Editorial, open feel | Border ≥ 3:1 when focused (`primary`); label always visible (never placeholder-only); error text under the field |
| Buttons are flat at every state, no state layer or elevation | M3 state layers / elevated buttons | Calm, luxury feel | Pressed state is a visible colour change (`primary` → `primary-container`); disabled is distinguishable without colour alone |
| Only cards cast a shadow; everything else is tonal | M3 elevation levels | One deliberate depth cue | Card shadow is warm-tinted and soft; no other component adds shadow |
| Custom fonts (Playfair Display, DM Sans), not Roboto/SF Pro | HIG system font | Brand identity | Scales with font scale / Dynamic Type; no clipping at 2× |
| Material Symbols on iOS, not SF Symbols | HIG iconography | One icon set, one codebase | Light weight (100–300); every icon-only control has a label |
| Gold `accent` is decorative only on cream | — | Gold on cream is 1.81:1 | Never text or a meaningful icon on cream; text on gold uses `on-accent` (8.1:1) |
| Glass (Haze) on content: fragrance notes cards | HIG Liquid Glass guidance (control layer only) | Signature material of the brand | Text contrast computed with no blur; solid fallback under Reduce Transparency; no essential info carried by refraction |
| No FAB; create post lives in the header | M3 FAB for primary creation | FAB fights the video overlay and is Android-only idiom | Create action reachable from every tab root within one tap |
| Inline links use colour (`interactive`), no underline unless hovered (current `design.md`) | WCAG 1.4.1 (colour alone) | Quiet editorial look | **Proposed change, needs your decision.** `interactive` vs body ink is 2.65:1 (needs 3:1 to rely on colour alone), and "hover" doesn't exist on touch. Proposal: underline inline links inside body text; standalone links (own line, button-like) can stay plain. This conflicts with `design.md` and `ads-design-system.md` as written, so it isn't a rule until those change |

---

## 6. Component map

Legend: **Share** = same on both platforms · **Adapt** = behaviour differs per platform now · 🧪 = Cupertino spike candidate.
The A11y column names the `ui/accessibility` Modifier extension that normally fits (see ADS-STE100 → Accessibility).

### 6.0 Use the repo's shared components first

`composeApp/src/commonMain/kotlin/ui/components/` already wraps most of what follows. The Material names in the tables below are what those wrappers build on, and what to reach for only when no shared component exists. Names verified at `72925dd`; open the file for the exact API.

| Need | Shared component |
|---|---|
| Bottom navigation | `BottomNavigation.kt`, `BlendedBottomNav.kt` |
| Top bars / headers / scaffold | `TopAppBar.kt`, `StackedTopBar.kt`, `BlendedHeader.kt`, `BlendedScaffold.kt` |
| Back button | `BackButton.kt` |
| Tabs | `ScentTabRow.kt`, `TopTabs.kt` |
| Buttons | `buttons/ScentButtons.kt` (primary, secondary), `buttons/ScentSocialButton.kt` |
| Text fields, labels, sliders | `ScentInputFields.kt`, `FieldLabel.kt`, `LabelledSlider.kt`, `SearchEntryField.kt` |
| Chips / filters | `Chip.kt`, `ChoiceChips.kt`, `FilterButton.kt` |
| Confirm dialog | `ScentConfirmDialog.kt` |
| Cards / rows | `FragranceCard.kt`, `ListingCard.kt`, `ReviewCard.kt`, `PostTile.kt`, `ProfileListingRow.kt`, `RaisedSection.kt` |
| Loading / empty / error | `Shimmer.kt`, `EmptyState.kt`, `ErrorState.kt` |
| Dividers | `ScentDivider.kt` |
| Video | `VideoPlayer.kt` (+ `androidMain` / `iosMain` actuals) |

**Not in the repo yet:** `GlassSurface` / Haze, pull-to-refresh, haptics, a bottom sheet component (one `ModalBottomSheet` use exists), a date picker. Rows that depend on them say so.

### Navigation

| User need | Component (CMP / M3) | Scent treatment | iOS | A11y |
|---|---|---|---|---|
| Move between top-level areas (Home, Search, Profile, Market) | `NavigationBar` + `NavigationBarItem` via `BottomNavigation` / `BlendedBottomNav` | `surface` container, `primary` active indicator, Material Symbols Outlined (weight 100–300), labels always visible | Share. iOS 26 uses a floating glass capsule; ours stays a flat bar until the spike compares it with a `GlassSurface` capsule 🧪 | `accessibleState` (selected) per item |
| Screen header on a pushed/detail screen | `CenterAlignedTopAppBar` | `surface` background, title in `headline-sm`, no elevation | Share (centred titles read as native on iOS too) | heading semantics on title |
| Header on a tab root (Feed, Search, Profile, Market) | `TopAppBar` or `LargeTopAppBar` collapsing on scroll | Lowercase Playfair wordmark; `display-brand` scale only on brand moments | Share | heading semantics |
| Go back | `BackButton` + `SystemBackHandler` | Per section 2 | Share today; chevron/swipe-back questions are **open** (section 2) | `accessibleLabel("Back")` |
| Switch views inside a screen (e.g. Profile: Posts / Reviews / Listings) | `PrimaryTabRow` (3+ views) or `SingleChoiceSegmentedButtonRow` (2–3 short views) | `primary` indicator, `label-uppercase` text | Share; segmented controls are idiomatic on iOS, prefer them for 2–3 options | `accessibleState` (selected) |
| Search | `SearchBar` / `DockedSearchBar` at the top of the Search tab | Bottom-border field style, `interactive` clear action | Share | `accessibleLabel` on the field |

### Actions

| User need | Component | Scent treatment | iOS | A11y |
|---|---|---|---|---|
| The one primary action on a screen | `Button` (shared primary wrapper in `ui/components/`) | `primary` fill, `on-primary` text, 52dp tall, 12dp radius, flat | Share | `accessibleClickable` with a verb label |
| Secondary action | `OutlinedButton` (shared secondary wrapper) | 1dp `primary` border, `primary` text | Share | `accessibleClickable` |
| Inline / tertiary action, FOLLOW | `TextButton` | `interactive` (#7E5700), weight 500 | Share | `accessibleClickable` |
| Inline link inside body text | `LinkAnnotation` in `AnnotatedString` | `interactive`; underline is **proposed** (section 5) | Share | link role |
| Icon-only action (share, more, close) | `IconButton` | `on-surface` icon, 48dp target | Share | `accessibleLabel` (required) |
| Like / save | `IconToggleButton` | Liked heart uses `error`; saved uses `primary` fill (gold icon on cream fails 3:1) | Share; haptic on toggle (not adopted yet) | `accessibleToggle` |
| Create a post | Header action on tab roots | `primary` icon | Share. **No FAB** (section 5) | `accessibleLabel("Create post")` |
| More actions on a post/listing | `ModalBottomSheet` with `ListItem` rows | `surface-container-low` sheet, destructive row in `error` | Share. Avoid `DropdownMenu` as the main overflow pattern | `accessiblePane` on the sheet; `withCustomActions` on the card |

### Selection & input

| User need | Component | Scent treatment | iOS | A11y |
|---|---|---|---|---|
| Type text (auth, captions, listing details) | `TextField` with indicator-only styling, label above | Bottom border `outline-variant` → `primary` on focus, `label-uppercase` label, `error` helper text under the field | Share; `KeyboardOptions` (type + `imeAction`) and `imePadding()` on every form | label merged into the field; error announced |
| Filter a list (brand, concentration, price band) | `FilterChip` in a `LazyRow` | Selected = `accent` fill + `on-accent` text; unselected = `outline-variant` border | Share | `accessibleState` (selected) |
| Pick one of 2–3 options | `SingleChoiceSegmentedButtonRow` | Selected segment `primary` fill + `on-primary` text; `outline-variant` borders | Share | `accessibleState` |
| Pick one of many (bottle size, condition) | ≤6 options: `FilterChip` group; more: `ModalBottomSheet` with `RadioButton` rows | | Share. Avoid `ExposedDropdownMenuBox` (reads as Android/web) | `collectionContainer` / `collectionItem` |
| Pick several | `Checkbox` rows in a sheet, or multi-select chips | | Share | `accessibleState` |
| On/off setting | `Switch` | `primary` track when on | 🧪 M3 switch shape differs from the iOS toggle | `accessibleToggle` |
| Choose a price range | `RangeSlider` | `primary` thumbs and track | Share | slider semantics + value text |
| Choose a date | `DatePicker` in `DatePickerDialog` | `primary` selection | 🧪 Most foreign-feeling M3 component on iOS | dialog pane title |
| Rate a fragrance | Custom star row in `ui/components/` | `accent` stars, always paired with the numeric rating in `on-surface` (gold alone fails 3:1 on cream) | Share | single adjustable element, `accessibleState("3 of 5 stars")` |

### Containment & content

| User need | Component | Scent treatment | iOS | A11y |
|---|---|---|---|---|
| Fragrance, listing or post summary | `Card` / `ElevatedCard` | The one sanctioned shadow: soft, warm-tinted; 12dp radius | Share | `clearedDescription` or `mergedGroup` |
| List of rows | `LazyColumn` + `ListItem` | 1dp `outline-variant` dividers, no shadow | Share | `collectionContainer` / `collectionItem` |
| Short-form video feed | `VerticalPager` | Overlays via `GlassSurface` (**planned**; Haze glass, solid under Reduce Transparency); see the *Glass feed overlay* ticket | Share | one announcement per post; actions via `withCustomActions` |
| Image gallery (bottle shots) | `HorizontalPager` + page indicator (or `HorizontalMultiBrowseCarousel`) | 12dp radius images | Share | `accessibleLabel` per image; indicator not focusable |
| Fragrance notes (top/heart/base) | Glass notes cards (`GlassSurface`, **planned**) | Per the glass notes ticket; deviation in section 5 | Share | `mergedGroup` per tier |
| Confirm something destructive | `ScentConfirmDialog` (wraps `AlertDialog`) | Verb-labelled confirm in `error`, Cancel as `TextButton`; title states the consequence | 🧪 Button styling/order differs from iOS alerts | dialog pane title; focus moves into the dialog |

### Feedback & status

| User need | Component | Scent treatment | iOS | A11y |
|---|---|---|---|---|
| Content is loading | `Shimmer` skeleton placeholders matching the final layout | `surface-container` blocks; shimmer off under reduced motion | Share | one "Loading" announcement, skeletons non-semantic |
| Short blocking wait (submit) | Inline `CircularProgressIndicator` inside the button | Button keeps its width | Share | busy state on the button |
| Refresh a feed/list | `PullToRefreshBox` (not used yet) | `primary` indicator | Share | refresh as a custom action |
| Transient confirmation ("Listing saved") | `Snackbar` via `SnackbarHost`, above the nav bar | `inverse-surface` container, `inverse-on-surface` text, one optional action | 🧪 iOS has no snackbar convention; acceptable until the spike | polite live region |
| Error the user can fix | Inline error under the field / `ErrorCard` in content | `error` / `error-container` (ADS-STE100 error display) | Share | error announced; focus to first invalid field |
| Nothing to show yet | `EmptyState` (`ui/components/EmptyState.kt`) | Icon or illustration, `headline-sm` title, one-line body, one action | Share | `mergedGroup` |
| Counts, verified seller, unread | `Badge` / `BadgedBox` | `accent` fill, `on-accent` text | Share | included in the parent's label |

**Not in the map?** Don't improvise. Check current M3 and HIG guidance (section 12), pick a component, add a row here, and record any adapt-or-share call as an ADR (section 11).

---

## 7. Mobile layout rules

1. **One primary action per screen.** On task screens (checkout, create post, auth) pin it to the bottom, inside the thumb zone and above the keyboard.
2. **Touch targets ≥ 48dp** (covers iOS's 44pt). Buttons are 52dp per `design.md`. The glass feed overlay ticket says ≥44dp; use 48dp.
3. **Every screen designs four states** before it's built: loading, content, empty, error. Add offline and signed-out where they apply.
4. **Progressive disclosure:** secondary detail goes in sheets or a detail screen, not on the card.
5. **Content first on media screens:** the feed has no persistent chrome beyond the nav bar and glass overlays.
6. **Works at 2× font scale** with nothing clipped and every CTA reachable by scrolling.
7. **No component outside the map** without a note in the screen spec explaining why. Then add it to section 6.

---

## 8. Screen archetypes

Start a new screen from the closest archetype.

| Archetype | Backbone | Key components | States that matter |
|---|---|---|---|
| Video feed (Home) | `VerticalPager` | Glass post panel, action capsules (`IconToggleButton`), fragrance tag chip | Loading skeleton, no posts, video failed, offline |
| Fragrance detail | `LazyColumn` with a collapsing header | Image pager, notes cards, rating row, review list, sticky "See listings" `Button` | Loading, no reviews, discontinued |
| Marketplace listings | `LazyColumn` / grid | `FilterChip` row, listing `Card`s, `PullToRefreshBox` | Loading, no results for filters (offer "clear filters"), error |
| Listing detail + buy | `LazyColumn` | Image pager, seller row, condition/size chips, pinned primary `Button` | Sold, reserved, own listing (edit instead of buy) |
| Search | `SearchBar` + results list | Recent searches, `FilterChip`s, result `ListItem`s | Empty query, no results, loading |
| Profile | Header + `PrimaryTabRow` | Avatar, counts, Posts/Reviews/Listings tabs | Own vs other profile, empty tabs |
| Create post | Full-screen task flow | Media picker, caption `TextField`, fragrance tag picker (sheet), Cancel + pinned "Post" `Button` | Draft, uploading (progress), upload failed, discard confirm |
| Auth | Fixed 384dp column | Bottom-border `TextField`s, primary + social secondary buttons | Validation errors per field, submitting, server error |
| Checkout | Full-screen task flow | Summary `Card`, address/payment rows, Cancel + confirm `Button` | Processing, payment failed, success |

---

## 9. Screen spec (output of design mode)

Every new UI ticket gets one. It goes in the Notion ticket body and is what a Claude Design brief is built from.

```markdown
## Screen spec — <ScreenName>
**Archetype:** <section 8>   **Tab / entry:** <how the user gets here>   **Exit:** <where back goes, per platform>
**Job:** <one sentence>   **Primary action:** <one>

### Component map
| Element | Component (section 6) | Notes / tokens |
|---|---|---|

### States
- Loading: …
- Content: …
- Empty: …
- Error: …
- (Offline / signed-out / other)

### Platform notes
- Android: …
- iOS: … (back behaviour, any 🧪 components in use)

### Deviations used
- <row from section 5, or "none">

### Accessibility
- <element> → <ui/accessibility extension> + label
```

## 10. Turning the spec into a Claude Design brief

- **Frames:** design an iPhone frame first (it's the platform without native-for-free), then check the Android frame.
- **Tokens:** attach `design.md`. Ask for M3 token names only, Playfair Display lowercase for brand moments, DM Sans for everything else, Material Symbols Outlined at light weight.
- **Components:** name each element by its component from section 6 ("a `FilterChip` row", "a `ModalBottomSheet` with `ListItem` rows"), not a visual description. This stops the design inventing components that don't exist in Compose.
- **States:** every state in the spec as its own frame, plus a 2× font-scale frame of the content state.
- **Constraints to state explicitly:** 48dp targets, one primary action, no FAB, no hover states, flat buttons, gold `accent` never used for text on cream. Add "underlined inline links in body text" only once section 5's proposal is accepted.
- **Treat the output as reference.** The built screen uses the components in this doc. If the design needs something that isn't here, add it to section 6 first.

---

## 11. Decision log

Adapt-or-share calls and changes to this doc are recorded as ADRs in `docs/adr/` via `decision-tradeoffs`.

| Decision | ADR | Status |
|---|---|---|
| Material 3 on both platforms; iOS adapts behaviour only | *To record* (**ADR-0004 is reserved** for the server-driven Fragrance Detail ADR; next free number is 0005) | Accepted 2026-11-08 |
| Adopt Cupertino widgets for 🧪 components? | Spike output | Pending (spike ticket above) |

## 12. Keeping current

- **Sources of truth for platform rules:** Material 3 (m3.material.io) and Apple's Human Interface Guidelines (developer.apple.com/design/human-interface-guidelines). Check them when a component isn't in the map, or when a rule here is being challenged. Don't rely on memory for either; both change every year.
- **`mobile-ecosystem-watch`** flags Material, HIG and Compose design-system changes. A Tier 1 item that touches this doc becomes a ticket to update it.
- **Each new iOS / Android major release:** re-check sections 2–4 and the 🧪 rows.

## 13. Known drift to fix

- `design.md` describes **hover** states for buttons and links. On mobile these should read **pressed**.
- `design.md` and `ads-design-system.md` say links are underlined on hover only. Section 5 proposes underlining inline body links.
- `design.md` references `transition-default` (300ms) but doesn't define it in the token front-matter.
- iOS edge swipe at a non-Home tab root switches to Home under the shared policy (section 2). Decide whether that's acceptable; it's an ADS-STE100 question, not a doc edit.
- The glass feed overlay ticket uses a ≥44dp target floor; this doc uses 48dp.
