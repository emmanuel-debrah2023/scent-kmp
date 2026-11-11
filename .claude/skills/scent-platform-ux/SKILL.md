---
name: scent-platform-ux
description: Design new Scent screens or review Scent UI against docs/platform-guidelines.md (Material 3 + Apple HIG under Compose Multiplatform). Use for new screens, UI tickets, Claude Design briefs and UI reviews, and when unsure which component or platform behaviour to use.
---

# Scent Platform UX

Designs new Scent screens and reviews Scent UI so they suit mobile on both Android and iOS, use the right components, and follow the Material 3 and Apple HIG rules the project has adopted.

**The rules live in `docs/platform-guidelines.md`, not in this skill.** This skill is the procedure; the doc is the rulebook. If they disagree, the doc wins: follow it and flag the drift. Like ADS-STE100, if you think the doc is wrong, say so explicitly rather than silently deviating.

Two modes:

| Mode | Use when | Produces |
|---|---|---|
| **A — Design** | A new screen, a new UI ticket, a big rework, "what should this screen be", before going to Claude Design | A screen spec + a ready-to-paste Claude Design brief |
| **B — Review** | A Claude Design export or screenshot, a HotSwan capture, a Compose diff, or a spec to check | A findings table and a verdict |

Use Mode A before Mode B when a screen doesn't exist yet. Not for: visual taste critique (`design:design-critique`), a full WCAG audit (`design:accessibility-review`), or code structure (ADS-STE100, via `scent-dev-loop` and `ads-ste100-auditor`).

---

## Step 0 — Load the rules

Read, in order:

1. `docs/platform-guidelines.md`. If the repo isn't available, use the project copy `claude/platform-guidelines.md`. If neither is reachable, stop and say so. Don't work from remembered rules; both platforms change them every year.
2. `docs/design.md` and `.claude/rules/ads-design-system.md` for tokens, type and shape.
3. The Accessibility section of `docs/architecture-guidelines.md` (ADS-STE100) for which `ui/accessibility` extension fits which pattern.
4. `docs/adr/` for any ADR newer than the guidelines doc's "Decided" date, especially the Cupertino spike's. An accepted ADR binds like ADS-STE100 and overrides the 🧪 rows; flag that the doc needs updating.

The doc's sections: 1 decision, 2 shared vs adapted, 3 Material rules, 4 HIG rules, 5 brand deviations, 6 component map (6.0 = existing shared components), 7 layout rules, 8 archetypes, 9 spec template, 10 Claude Design brief, 11 decision log.

---

## Mode A — Design a screen

1. **Brief.** Pin down the screen's job (one sentence), its one primary action, how the user gets in, where back goes on each platform, and what data it shows. If the primary action is genuinely unclear, ask one question; otherwise state your assumption and carry on.
2. **Archetype.** Pick the closest archetype from section 8 and start from its backbone and states.
3. **Component map.** Map every element to a component by name, **preferring an existing shared component** in `composeApp/src/commonMain/kotlin/ui/components/` (section 6.0), then the section 6 Material component. Check the file exists before naming it. Never invent a component to fill a gap, and never name something the doc marks "planned" or "not used yet" as if it were available.
4. **States.** Loading, content, empty and error, plus whatever the archetype lists (sold, reserved, own listing…). Each state says what the user sees and what they can do.
5. **Platform notes.** Apply section 2: back behaviour on each platform, safe areas, keyboard, tab bar visibility. List any 🧪 components used; they ship as Material until a spike ADR says otherwise.
6. **Deviations used.** List the section 5 rows the screen relies on and confirm each guard holds (gold never carries text on cream; inputs keep a visible label; and so on). Section 5's underlined-inline-link item is a *proposal*, not a rule, until `design.md` changes.
7. **Accessibility.** For each interactive or grouped element, name the `ui/accessibility` extension and its label. Check the screen still works at 2× font scale: nothing clipped, every CTA reachable.
8. **Output.**
   - The **screen spec**, using the section 9 template exactly.
   - A **Claude Design brief** built from it, following section 10, in one fenced block ready to paste: iPhone frame first, then Android; attach `design.md`; name components, not visuals; one frame per state plus a 2× font-scale frame; the section 10 constraints spelled out.
   - Offer to put the spec on the Notion ticket via `scent-ticket` (page body, under `## Screen spec`).

Don't draw the screen yourself unless asked; Claude Design does the visuals. The spec is the contract the build is checked against.

---

## Mode B — Review

Work through these in order and cite the doc section and rule number for every finding:

1. **Components** — each element maps to a shared component, a section 6 component, or the ticket's spec. Off-map or invented components are findings.
2. **Platform behaviour** — section 2: back per platform, insets rather than hardcoded padding, keyboard handling, pressed not hover, tab bar visibility.
3. **Material and HIG rules** — sections 3 and 4, by number.
4. **Deviations** — a deviation listed in section 5 is fine if its guard holds. One that isn't listed is a finding.
5. **States and layout** — section 7: one primary action, ≥48dp targets, all four states designed, 2× font scale.
6. **Accessibility quick pass** — labels, targets and contrast. When the token pair is known, compute the ratio rather than eyeballing it. Send anything deeper to `design:accessibility-review`.

For a Compose diff this complements `scent-dev-loop` Gate 2; don't repeat ADS-STE100 code-structure checks here.

**Output:**

| Severity | Element | Rule | Fix |
|---|---|---|---|
| Blocker / Should fix / Nit | … | e.g. §2 Back, §4.2 | concrete change |

Lead with blockers. End with a verdict: **Ready**, **Ready with fixes**, or **Not ready**. If it's clean, say so in one line; don't pad the table.

---

## Unknown component, or a rule in doubt

Use this when an element isn't in section 6, a rule is being challenged, a new iOS or Android major release has shipped, or the doc's review date has passed.

1. **Look up current guidance** from official sources first: m3.material.io and developer.apple.com/design/human-interface-guidelines (WebSearch / WebFetch). Cite what you used. Also confirm the component exists in Compose Multiplatform's `compose.material3`, not only in Android Jetpack.
2. **Decide share vs adapt** with the section 1 principle: brand decides the look, the platform decides behaviour.
3. **Record it.** A new adapt-or-share call or brand deviation goes to `decision-tradeoffs`: Mode A if there's a real fork to compare, Mode B to write the ADR in `docs/adr/`. Check the numbering table in `docs/adr/README.md` first; ADR-0004 is reserved.
4. **Update the doc.** Propose the exact edit to `docs/platform-guidelines.md` (a new row in section 6, 2 or 5, plus an entry in section 11) and show it before applying. A gap outside the current ticket's scope becomes a Tasks Tracker ticket via `scent-ticket`, not a bigger PR.

Never add a component or change a rule silently inside a ticket or a spec.

**Don't touch the back-press policy.** It's governed by ADS-STE100 and `.claude/rules/ads-navigation.md` ("do not modify without being asked"). If a platform question exposes a problem with it, raise it as a decision.

---

## Hand-offs

- `scent-ticket` — puts the spec on the ticket and writes the UI acceptance criteria.
- `scent-dev-loop` — builds it; Gate 2 checks the code against the spec and the doc.
- `ads-ste100-auditor` — code-structure compliance with ADS-STE100.
- `design:accessibility-review` — full WCAG 2.1 AA audit.
- `design:design-critique` — hierarchy and visual critique beyond platform rules.
- `decision-tradeoffs` — ADRs for adapt-or-share calls.
- `mobile-ecosystem-watch` — Material / HIG / Compose changes that may need the doc updated.

## Rules of thumb

- The doc beats memory, and the doc beats this skill.
- Reference doc sections in tickets and specs; don't copy the tables into them.
- 🧪 components ship as Material until an accepted ADR says otherwise.
- The brand look stays the same on both platforms; behaviour follows each platform.
