---
paths:
  - "composeApp/src/**/ui/**"
  - "composeApp/src/**/*Screen.kt"
  - "composeApp/src/**/*Composable.kt"
  - "**/ui/components/**"
  - "**/ui/theme/**"
---

# Scent Minimalist Luxury — design system

This rule covers what components *look like*. `compose-component-api.md` covers
what their *signatures* look like — both load together on UI edits.

The single source of truth is the M3 token set in `ui/theme/`. There is no
parallel custom token layer; every colour reference uses an M3 role name.

**Never hardcode.** No hex literals, no `Color(0xFF...)`, no bare `.dp` spacing
values outside the theme, no `TextStyle(...)` constructed inline. Reach for
`MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, and the spacing
tokens. If a value you need has no token, add the token — do not inline it.

**Colour role separation is deliberate — do not collapse these:**
- `primary` (forest green) — brand, primary buttons, active nav, focus states.
- `accent` (gold) — decoration only: rating stars, badges, dividers, icon fills,
  selected chips.
- `interactive` (gold-brown) — inline links and text-level tappable affordances,
  weight 500, underline on hover only.

**Gold is a contrast trap.** Gold on the cream `surface` is ~1.8:1. On cream it
may be used as a fill or shape only — never as text, never as a thin
interactive outline. Gold *text* is permitted only on dark surfaces (`primary`,
`on-surface` ink, `inverse-surface`). Text sitting on a gold fill uses
`on-accent`.

**Elevation.**
- Cards get the one sanctioned shadow: soft, warm/brown-tinted, low opacity,
  generous blur, minimal offset.
- Buttons are flat at every state — default, hover, pressed, disabled. State is
  communicated through colour only. A button with elevation is a bug.
- Everything else separates via 1px `outlineVariant` borders and tonal
  `surfaceContainer*` layering, not shadow.

**Shape and components.**
- Inputs: bottom border only, `outlineVariant` at rest, transitioning to
  `primary` on focus. Uppercase `label-uppercase` label above the field.
- Containers and buttons: 12px (`rounded-md`) radius.
- Touch targets: `button-height` (52px). Focused/auth layouts cap at
  `auth-max-width` (384px).
- Vertical rhythm: `section-gap` (64px) between sections, `element-gap` (32px)
  within one.
- Icons: Material Symbols Outlined, weight axis 100–300 to match the Playfair
  serif weight. No other icon set.
- All colour and border transitions run 300ms ease-in-out.

**Typography.** Playfair Display, **lowercase**, for brand-level display
moments only. DM Sans for everything functional. Labels are uppercase with
0.1em tracking.

**Social sign-in buttons** are secondary-button styling with a ≤20px icon.
Google and Apple handlers stay stubbed — backend work is roadmap Phase 2/3
(`docs/auth.md`). Do not wire them up as a side effect of a UI change.
