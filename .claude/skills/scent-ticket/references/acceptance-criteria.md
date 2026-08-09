# Acceptance criteria patterns

Every ticket gets 2–5 checkable conditions plus an ADS-STE100 checkpoint naming
the facets that ticket actually touches. Naming the specific facets matters —
"complies with ADS-STE100" pasted onto every ticket stops carrying information.

## The ADS-STE100 facets

Pick the ones relevant to the ticket:

| Facet | Use when the ticket touches |
|---|---|
| null-safety boundary | DTOs, mappers, domain models |
| Either error handling | repositories, use cases, validators |
| Koin DI | any new class that needs registering |
| UiState pattern | ViewModels, screen state |
| per-tab navigation | new screens or routes |
| design tokens | any UI work |
| test conventions | anything with business logic |

## By ticket type

### 🐞 Bug
```
- <the incorrect behaviour> no longer occurs when <trigger condition>
- <the correct behaviour> happens instead
- Regression test covers the failing case
- Complies with ADS-STE100 (<facets>)
- ktlint and detekt pass with no new violations
```

### 💬 Feature request
```
- <primary user-visible capability> works end to end
- <edge case or empty state> is handled
- Renders correctly on both Android and iOS targets
- Unit tests cover the ViewModel's loading/error/success states
- Complies with ADS-STE100 (<facets>)
```

### 💅 Polish
```
- <element> matches the design system spec (<specific token or rule>)
- No hardcoded colour or dimension values introduced
- Behaviour unchanged — this is visual only
- Complies with ADS-STE100 (design tokens)
```

### 📝 Docs
```
- <document> exists at <path> and covers <the specific gap>
- Code examples in it compile against the current codebase
- Linked from <wherever a reader would start>
```

### 🔧 Tech Task
```
- <the structural change> is complete
- All existing tests still pass; no behavioural change
- <the thing that motivated it> is no longer possible / no longer required
- Complies with ADS-STE100 (<facets>)
```

## Anti-patterns

- "Works correctly" / "Is implemented" / "Looks good" — not checkable.
- Restating the description as a single bullet.
- Ten bullets. If it needs ten, it's two tickets.
- Boilerplate ADS-STE100 listing all seven facets on a one-line CSS fix.
- Acceptance criteria that only the person who wrote the ticket can evaluate.
