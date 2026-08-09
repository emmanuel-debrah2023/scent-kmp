---
paths:
  - "**/di/**"
  - "**/*Module.kt"
  - "**/FragranceApplication.kt"
  - "**/*Application.kt"
---

# ADS-STE100 — Dependency injection with Koin

**Constructor injection, always.** A class declares what it needs in its
constructor. No `get<T>()`, `inject()`, or `by inject()` inside a class body —
that is service location and it makes the class untestable.

**Module placement.** Every new binding goes in the module that matches its
layer, and nowhere else:

| Binding | Module |
|---|---|
| `HttpClient`, `ApiClient`, `TokenManager` | `networkModule` |
| Room database, DAOs, DataStore | `databaseModule` |
| `*RepositoryImpl` bound to its interface | `repositoryModule` |
| `*UseCase` | `useCaseModule` |
| `*ViewModel` | `viewModelModule` |

**Scopes.** `single { }` for repositories, clients, DAOs, and anything holding
state or a connection. `factory { }` for use cases. `viewModel { }` for
ViewModels. A use case registered as `single` is a bug.

**Bind to the interface**, not the implementation:
`single<FragranceRepository> { FragranceRepositoryImpl(...) }`. Consumers depend
on `FragranceRepository`.

**Parameterised ViewModels** (a detail screen needing an id) use
`viewModel { parameters -> XViewModel(id = parameters.get(), ...) }` — do not
smuggle the id in through a singleton holder.

When adding a class, add its binding in the same change. A class that compiles
but was never registered fails at runtime, not at build time — that is what this
rule exists to catch.
