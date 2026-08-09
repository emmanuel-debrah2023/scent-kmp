---
paths:
  - "shared/src/commonMain/kotlin/**/repository/**"
  - "shared/src/commonMain/kotlin/**/usecase/**"
  - "shared/src/commonMain/kotlin/**/domain/error/**"
  - "shared/src/commonMain/kotlin/**/domain/validation/**"
  - "**/*Repository.kt"
  - "**/*RepositoryImpl.kt"
  - "**/*UseCase.kt"
  - "**/Validator.kt"
---

# ADS-STE100 — Error handling with Either

`typealias Result<T> = Either<AppError, T>`. Every operation that can fail
returns it. Expected failures are values, not exceptions.

**Signatures**
- Every repository interface method and every use-case `invoke` returns `Result<T>`.
- Construct with `.asRight()` / `.asLeft()` — not `Either.Right(...)` directly.
- Use cases validate their params *before* touching the repository and return
  `AppError.ValidationError.*` on failure.

**Errors**
- Only ever use members of the sealed `AppError` hierarchy: `NetworkError`,
  `AuthError`, `ValidationError`, `ContentError`, `StorageError`, `Unknown`.
  Adding a new failure mode means adding a subclass, not a new string.
- Never surface a raw HTTP status code past the repository. Map it:
  `404` → the relevant `ContentError.*NotFound`, `5xx` → `NetworkError.ServerError`.
- `AppError.Unknown` is a last resort, and always carries `cause`.

**try/catch**
- Permitted only at the outermost edge of a repository method, to convert a
  thrown platform exception into a `Left`. Never for control flow.
- Catch the specific ones first (`SerializationException`, `UnknownHostException`,
  `SocketTimeoutException`); a bare `catch (e: Exception)` must re-wrap into
  `AppError.Unknown(cause = e)` rather than swallow.

**Never** return `null` to mean failure, and never let a `Left` be silently
discarded — every call site either folds it or propagates it.
