---
paths:
  - "**/*.kt"
  - "**/*.kts"
---

# Idiomatic Kotlin

This loads on every Kotlin edit, so it is deliberately short. It covers what no
linter can check — the mechanical subset lives in detekt (Gate 3).

## Scent-specific traps

- **`Result<T>` is `typealias Result<T> = Either<AppError, T>`.** Never
  `kotlin.Result`, never `runCatching { }`. They type-check against the wrong
  thing and read as correct.
- **`docs/auth.md` server snippets use `java.time.LocalDateTime`.** Correct
  there — the server is JVM-only. Copying that pattern into `shared` breaks the
  iOS build.

## Structure

- **Top-level and extension functions over utility classes.** No `FooUtils`,
  `FooHelper`, or `FooManager` holding static-ish methods. If it doesn't hold
  state, it isn't a class.
- **`object` for stateless singletons**, not a class with a private constructor
  and a companion `getInstance()`.
- **Default and named arguments over builders and overloads.** Three overloads
  differing by parameter count is a Java habit.
- **`data class` for value holders** — never hand-written `equals`/`hashCode`/
  `toString`. Prefer a `data class` over `Pair`/`Triple` anywhere the components
  have meaning; `first`/`second` at a call site is a smell.
- **A function type beats a single-method interface.** `(String) -> Unit`, not
  `interface OnThingListener`.
- **`sealed` + exhaustive `when`** over `if/else if` chains on type. Omit the
  `else` branch when the `when` is exhaustive, so adding a case becomes a
  compile error rather than a silent fallthrough.

## Properties and null

- **Properties, not getters/setters.** `val name: String`, not `getName()`.
  Computed values use a `get()` accessor or an extension property.
- **`lateinit var` is a last resort**, not a default. Prefer constructor
  injection, `by lazy`, or a nullable with a safe accessor.
- Chain safe calls with `?.` and `?:`; don't nest `if (x != null)` blocks.
- `if (nullableBool == true)` is a code smell — restructure so the nullability is
  handled where it arises.
- Scope functions carry meaning: `let` for transform-if-non-null, `apply` for
  configuring and returning the receiver, `also` for side effects, `run` for a
  block returning a result, `with` for grouped calls on one receiver. Don't chain
  three of them; extract a named function instead.

## Collections

- **Prefer `map`/`filter`/`fold`/`groupBy`/`associateBy`** to building a
  `mutableListOf()` and looping. Use a loop when it genuinely reads better —
  early exit, index arithmetic, side effects — not by default.
- **Expose read-only types.** Public API returns `List`, `Set`, `Map`; keep
  `MutableList` private. Never expose a `MutableStateFlow` — expose
  `StateFlow` via `asStateFlow()`.
- `isEmpty()` / `isNotEmpty()` / `isNullOrEmpty()`, not `size == 0`.
- `firstOrNull { }`, not `filter { }.firstOrNull()`.
- `emptyList()` over `null` for "no results".

## Expressions

- `when` and `if` are expressions — assign from them rather than assigning
  inside each branch.
- String templates over concatenation; `trimIndent()` for multi-line.
- `require`/`check`/`error` over hand-rolled `if (x) throw IllegalArgumentException(...)`
  — though remember expected failures in this codebase are `Either.Left`, not
  throws. `require` is for genuine programmer error only.
- Prefer immutability: `val` unless reassignment is real, `List` unless mutation
  is real.

## Coroutines

- Suspend functions over callbacks. `Flow` over listener registration.
- Never `runBlocking` in production code, never `GlobalScope`.
- Structured concurrency: work runs in a scope with a lifecycle
  (`viewModelScope`, or a scope passed in), never a detached one.
