
name: Scent Minimalist Luxury
colors:
surface: '#f9ecdc'
surface-dim: '#e4d8c9'
surface-bright: '#fff8f3'
surface-container-lowest: '#ffffff'
surface-container-low: '#fef2e2'
surface-container: '#f3e6d7'
surface-container-high: '#ede1d1'
surface-container-highest: '#e4d8c9'
on-surface: '#201b11'
on-surface-variant: '#504536'
inverse-surface: '#362f25'
inverse-on-surface: '#fbefdf'
outline: '#827564'
outline-variant: '#d4c4b0'
surface-tint: '#1b4332'
primary: '#1b4332'
on-primary: '#ffffff'
primary-container: '#c6e7d6'
on-primary-container: '#002114'
inverse-primary: '#a0c9b4'
accent: '#d4af37'
on-accent: '#201b11'
interactive: '#7e5700'
on-interactive: '#ffffff'
secondary: '#5f5e5e'
on-secondary: '#ffffff'
secondary-container: '#e2dfdf'
on-secondary-container: '#636262'
tertiary: '#605e5a'
on-tertiary: '#ffffff'
tertiary-container: '#9e9b96'
on-tertiary-container: '#343330'
error: '#ba1a1a'
on-error: '#ffffff'
error-container: '#ffdad6'
on-error-container: '#93000a'
primary-fixed: '#c6e7d6'
primary-fixed-dim: '#a0c9b4'
on-primary-fixed: '#002114'
on-primary-fixed-variant: '#0a4a33'
secondary-fixed: '#e5e2e1'
secondary-fixed-dim: '#c8c6c6'
on-secondary-fixed: '#1c1b1c'
on-secondary-fixed-variant: '#474647'
tertiary-fixed: '#e6e2dc'
tertiary-fixed-dim: '#cac6c1'
on-tertiary-fixed: '#1c1c18'
on-tertiary-fixed-variant: '#484743'
background: '#f9ecdc'
on-background: '#201b11'
surface-variant: '#ede1d1'
typography:
display-brand:
fontFamily: Playfair Display
fontSize: 64px
fontWeight: '700'
lineHeight: '1'
letterSpacing: -0.05em
display-brand-mobile:
fontFamily: Playfair Display
fontSize: 48px
fontWeight: '700'
lineHeight: '1'
letterSpacing: -0.05em
headline-sm:
fontFamily: Playfair Display
fontSize: 24px
fontWeight: '400'
lineHeight: '1.2'
body-lg:
fontFamily: DM Sans
fontSize: 16px
fontWeight: '400'
lineHeight: '1.5'
body-md:
fontFamily: DM Sans
fontSize: 16px
fontWeight: '400'
lineHeight: '1.5'
label-uppercase:
fontFamily: DM Sans
fontSize: 16px
fontWeight: '500'
lineHeight: '1'
letterSpacing: 0.1em
button-text:
fontFamily: DM Sans
fontSize: 16px
fontWeight: '500'
lineHeight: '1'
letterSpacing: 0.15em
rounded:
sm: 0.25rem
DEFAULT: 0.5rem
md: 0.75rem
lg: 1rem
xl: 1.5rem
full: 9999px
spacing:
container-padding: 16px
section-gap: 64px
element-gap: 32px
form-step: 8px
button-height: 52px
auth-max-width: 384px
Brand & Style
The brand evokes a sense of understated luxury, sensory elegance, and artisanal quality. Targeted at a sophisticated audience, the UI prioritizes a "breathable" aesthetic that mirrors the experience of high-end perfumery or boutique lifestyle brands.
The design style is a blend of Editorial Minimalism and Modern Tactility. It uses significant whitespace, lowercase serif typography for brand identity, and a warm, organic color palette. Interaction is subtle, relying on smooth transitions and high-contrast focus states rather than heavy shadows or complex gradients — the one sanctioned shadow is the soft, warm-tinted lift on cards. The emotional response is one of calm, clarity, and premium craftsmanship.
Colors
The palette is rooted in earth-toned sophistication. All color references in code must use the M3 token names below — there are no parallel custom tokens. This is the single source of truth.

primary (Forest Green #1B4332): The brand color — the wordmark, primary buttons, active nav, and focus states. Represents boutique confidence and craft. White (on-primary) sits on it at 11:1 contrast.
accent (Gold #D4AF37): Decorative and expressive moments — rating stars, badges, dividers, icon fills, selected chips. Fills and shapes only on cream: gold on the surface cream is ~1.8:1 contrast, so on cream it may only be used as a fill or shape, never for text or thin interactive outlines. Gold text is permitted only on dark surfaces, where it passes WCAG AA — verified pairings: on primary green #1B4332 (5.3:1), on on-surface ink #201B11 (8.1:1), and on inverse-surface #362F25 (6.3:1). When text sits on a gold fill, use on-accent (#201B11).
interactive (Gold-Brown #7E5700): Inline links and text-level interactive affordances that need to read as tappable against cream without competing with the green primary. Reads at sufficient contrast for text on surface.
on-surface (Ink #201B11): High-contrast text. Primary text color.
on-surface-variant (Warm Grey #504536): Secondary text — labels, taglines, helper copy. Maintains hierarchy without competing with primary content.
surface / background (Cream #F9ECDC): Soft, warm background. Reduces eye strain, feels more organic than pure white.
outline-variant (Cream Border #D4C4B0): Soft 1px borders for inputs and dividers in their default (unfocused) state.
outline (Warm Grey-Brown #827564): Higher-contrast borders for elements that need more presence.
error (#BA1A1A): Validation and error states. Used sparingly.

The system uses a "fidelity" variant where colors are applied with high intentionality — borders sit at outline-variant until interacted with, at which point they shift to primary. Role separation is deliberate: green primary carries brand and structural weight, gold accent is expressive decoration, and gold-brown interactive handles inline links — do not collapse them back into one token.
Typography
Typography is the primary driver of the brand's editorial feel.

Display Typography: Use Playfair Display in lowercase for brand-level moments. Weight 700, tight-leaded, impactful.
Body & Functional Text: Use DM Sans for its clean, geometric, low-contrast appearance.
Labels: Heavy letter-spacing (0.1em) and uppercase styling create a distinct visual "zone" for metadata and form headers.
Mobile Scaling: The 64px display-brand scales down to 48px (display-brand-mobile) on screens smaller than 375px to maintain headline integrity.

Layout & Spacing
The system follows a Fixed Grid approach for utility screens (auth, settings) and a Fluid Grid for content discovery.

Max Width: Authentication and focused tasks are constrained to auth-max-width (384px).
Vertical Rhythm: section-gap (64px) between major sections; element-gap (32px) within sections.
Touch Targets: Inputs and buttons use button-height (52px) — comfortable touch target, sleek profile.

Elevation & Depth
This system uses Tonal Layering and Border Focus for most surfaces, with one deliberate exception: cards carry a soft, warm-tinted shadow.

Surface Hierarchy: Primary surfaces use surface. Nested surfaces and hover states use surface-container-low through surface-container-highest based on tonal prominence.
Cards: Cards lift off the surface with a soft, warm-tinted drop shadow (low opacity, warm/brown-tinted rather than neutral grey, generous blur, minimal offset). This is the one place Z-axis elevation is used.
Buttons stay flat at every state — primary, hover, pressed, and disabled. Buttons never take a shadow; their state changes are conveyed through color only.
Depth via Lines (non-card elements): Distinction between inputs, dividers, and list rows is created through 1px outline-variant borders, not shadow.
Active State (inputs): Signaled through color transitions (border shifting from outline-variant to primary) rather than lift.
Transitions: All interactive elements use transition-default (300ms ease-in-out) for color and border changes.

Shapes
The shape language is Refined Geometry.

Input Fields: Border-bottom only — creates an elegant, open feel.
Containers/Buttons: Consistent 12px (rounded-md) radius. Softens the otherwise sharp horizontal lines of the typography and input borders.
Icons: Material Symbols Outlined (loaded from Google Fonts) — no custom icon set exists. Configure them toward the light end of the weight axis (100–300) so they match the delicate weight of the Playfair Display serifs.

Components

Buttons (Primary): Solid primary background with on-primary text. Hover state uses primary-container background with on-primary-container text.
Buttons (Secondary): Transparent background with 1px primary border and primary text.
Input Fields: No background, single bottom border in outline-variant. Label above in uppercase label-uppercase style. On focus, border transitions to primary.
Dividers: 1px outline-variant line with centered text in on-surface-variant.
Social Sign-in Buttons: Secondary button style with integrated SVG icons. Icon size ≤ 20px. Visual treatment only — backend integration is roadmap Phase 2 (Google) and Phase 3 (Apple). Stub these handlers in generated code.
Links: Inline links use interactive (gold-brown #7E5700) with font weight 500. No underline unless hovered. Links use interactive rather than primary so link text stays legible on cream and reads distinctly from the green brand color.
