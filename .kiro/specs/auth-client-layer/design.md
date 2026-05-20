# Design Document: auth-client-layer

## Overview

This document describes the technical design for the Phase 1 Auth Client Layer in the KMP `shared/` module of the Scent app. The layer sits between the Ktor backend (already built) and the Android/iOS UI (built separately). It provides the `Either`-based error handling, domain models, repository, use cases, token storage, and Koin DI wiring that both platforms consume.

The existing `shared/` module has stub files using `kotlin.Result` and hardcoded URLs. All auth-related files are replaced; two out-of-scope use cases (`AppleAuthUseCase`, `GoogleAuthUseCase`) are deleted.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer (out of scope)               │
│              ViewModel collects Flow<AuthState>              │
└────────────────────────────┬────────────────────────────────┘
                             │ invoke use cases
┌────────────────────────────▼────────────────────────────────┐
│                        Use Cases                             │
│  LoginUseCase  RegisterUseCase  GetCurrentUserUseCase        │
│  LogoutUseCase  IsLoggedInUseCase  ObserveAuthStateUseCase   │
└────────────────────────────┬────────────────────────────────┘
                             │ delegates to
┌────────────────────────────▼────────────────────────────────┐
│                   AuthRepository (interface)                  │
│              AuthRepositoryImpl (implementation)             │
│  • validates via Validator                                   │
│  • calls AuthApi                                             │
│  • maps via AuthMapper                                       │
│  • persists token via TokenStorage                           │
│  • maintains MutableStateFlow<AuthState>                     │
└──────────┬──────────────────────────┬───────────────────────┘
           │                          │
┌──────────▼──────────┐   ┌──────────▼──────────────────────┐
│      AuthApi        │   │        TokenStorage              │
│  (Ktor HTTP calls)  │   │  (expect/actual — DataStore/     │
│  register / login   │   │   NSUserDefaults)                │
│  getCurrentUser     │   └─────────────────────────────────┘
└──────────┬──────────┘
           │ returns DTOs
┌──────────▼──────────┐
│     AuthMapper      │
│  DTO → AuthUser     │
│  returns Result<T>  │
└─────────────────────┘
```

**Data flow for login:**
1. ViewModel calls `LoginUseCase(email, password)`
2. UseCase delegates to `AuthRepositoryImpl.login()`
3. Repo validates via `Validator` → returns `ValidationError` immediately if invalid
4. Repo calls `AuthApi.login()` → raw `AuthResponse` DTO
5. Repo maps via `AuthMapper.toAuthUser()` → `Result<AuthUser>`
6. On success: `TokenStorage.saveToken()`, emit `AuthState.Authenticated(user)`, return `Right(user)`
7. On HTTP 401: return `AppError.AuthError.InvalidCredentials.asLeft()`
8. On exception: map to appropriate `AppError`, return `Left`

---

## Components and Interfaces

### File Map

| File | Package | Purpose |
|------|---------|---------|
| `domain/util/Either.kt` | `org.scent.project.domain.util` | Either type + asLeft/asRight + Result typealias |
| `domain/error/AppError.kt` | `org.scent.project.domain.error` | Sealed error hierarchy |
| `domain/validation/Validator.kt` | `org.scent.project.domain.validation` | Input validation |
| `domain/model/AuthUser.kt` | `org.scent.project.domain.model` | Domain model (replace) |
| `domain/model/AuthState.kt` | `org.scent.project.domain.model` | Auth state sealed class |
| `domain/repository/AuthRepository.kt` | `org.scent.project.domain.repository` | Interface (replace) |
| `domain/usecase/LoginUseCase.kt` | `org.scent.project.domain.usecase` | Thin wrapper (replace) |
| `domain/usecase/RegisterUseCase.kt` | `org.scent.project.domain.usecase` | Thin wrapper (replace) |
| `domain/usecase/GetCurrentUserUseCase.kt` | `org.scent.project.domain.usecase` | Thin wrapper (replace) |
| `domain/usecase/LogoutUseCase.kt` | `org.scent.project.domain.usecase` | Thin wrapper (replace) |
| `domain/usecase/IsLoggedInUseCase.kt` | `org.scent.project.domain.usecase` | New |
| `domain/usecase/ObserveAuthStateUseCase.kt` | `org.scent.project.domain.usecase` | New |
| `data/remote/JsonConfig.kt` | `org.scent.project.data.remote` | Lenient Json instance |
| `data/remote/HttpClientFactory.kt` | `org.scent.project.data.remote` | expect fun |
| `data/remote/dto/AuthDtos.kt` | `org.scent.project.data.remote.dto` | Nullable DTOs (replace) |
| `data/remote/api/AuthApi.kt` | `org.scent.project.data.remote.api` | Ktor calls (replace) |
| `data/local/TokenStorage.kt` | `org.scent.project.data.local` | Interface + expect class (replace) |
| `data/mapper/AuthMapper.kt` | `org.scent.project.data.mapper` | DTO → domain |
| `data/repository/AuthRepositoryImpl.kt` | `org.scent.project.data.repository` | Implementation (replace) |
| `di/SharedModule.kt` | `org.scent.project.di` | Koin module |
| `androidMain/.../data/remote/HttpClientFactory.android.kt` | same | OkHttp actual |
| `androidMain/.../data/local/TokenStorageFactory.android.kt` | same | DataStore actual |
| `iosMain/.../data/remote/HttpClientFactory.ios.kt` | same | Darwin actual |
| `iosMain/.../data/local/TokenStorageFactory.ios.kt` | same | NSUserDefaults actual |

**Deleted files:**
- `domain/usecase/AppleAuthUseCase.kt`
- `domain/usecase/GoogleAuthUseCase.kt`

---

## Data Models

### Either / Result

```kotlin
// domain/util/Either.kt
package org.scent.project.domain.util

import org.scent.project.domain.error.AppError

sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    val isRight get() = this is Right<R>
    val isLeft  get() = this is Left<L>

    inline fun <C> fold(ifLeft: (L) -> C, ifRight: (R) -> C): C = when (this) {
        is Left  -> ifLeft(value)
        is Right -> ifRight(value)
    }
    inline fun <C> map(f: (R) -> C): Either<L, C> = when (this) {
        is Left  -> Left(value)
        is Right -> Right(f(value))
    }
    inline fun <C> flatMap(f: (R) -> Either<L, C>): Either<L, C> = when (this) {
        is Left  -> Left(value)
        is Right -> f(value)
    }
    inline fun onRight(action: (R) -> Unit): Either<L, R> { if (this is Right) action(value); return this }
    inline fun onLeft(action: (L) -> Unit): Either<L, R>  { if (this is Left)  action(value); return this }
    fun getOrNull(): R? = if (this is Right) value else null
    fun leftOrNull(): L? = if (this is Left) value else null
}

fun <R> R.asRight(): Either<Nothing, R> = Either.Right(this)
fun <L> L.asLeft(): Either<L, Nothing>  = Either.Left(this)

typealias Result<T> = Either<AppError, T>
```

### AppError

```kotlin
// domain/error/AppError.kt
package org.scent.project.domain.error

sealed class AppError {
    abstract val message: String
    abstract val cause: Throwable?

    sealed class NetworkError : AppError() {
        data class NoConnection(
            override val message: String = "No internet connection available",
            override val cause: Throwable? = null
        ) : NetworkError()
        data class Timeout(
            override val message: String = "Request timed out. Please try again",
            override val cause: Throwable? = null
        ) : NetworkError()
        data class ServerError(
            val statusCode: Int,
            override val message: String = "Server error occurred (Code: $statusCode)",
            override val cause: Throwable? = null
        ) : NetworkError()
        data class ParseError(
            val fieldName: String? = null,
            override val message: String = "Failed to parse server response" +
                (if (fieldName != null) ": $fieldName" else ""),
            override val cause: Throwable? = null
        ) : NetworkError()
    }

    sealed class AuthError : AppError() {
        data class InvalidCredentials(
            override val message: String = "Invalid email or password",
            override val cause: Throwable? = null
        ) : AuthError()
        data class UserAlreadyExists(
            override val message: String = "An account with this email already exists",
            override val cause: Throwable? = null
        ) : AuthError()
        data class TokenExpired(
            override val message: String = "Your session has expired. Please login again",
            override val cause: Throwable? = null
        ) : AuthError()
        data class Unauthorized(
            override val message: String = "You are not authorized to perform this action",
            override val cause: Throwable? = null
        ) : AuthError()
    }

    sealed class ValidationError : AppError() {
        data class InvalidEmail(
            override val message: String = "Please enter a valid email address",
            override val cause: Throwable? = null
        ) : ValidationError()
        data class PasswordTooShort(
            val minLength: Int = 8,
            override val message: String = "Password must be at least $minLength characters",
            override val cause: Throwable? = null
        ) : ValidationError()
        data class RequiredFieldEmpty(
            val fieldName: String,
            override val message: String = "$fieldName is required",
            override val cause: Throwable? = null
        ) : ValidationError()
        data class InvalidInput(
            val fieldName: String,
            override val message: String = "Invalid $fieldName",
            override val cause: Throwable? = null
        ) : ValidationError()
    }

    sealed class StorageError : AppError() {
        data class ReadFailed(
            override val message: String = "Failed to read data from storage",
            override val cause: Throwable? = null
        ) : StorageError()
        data class WriteFailed(
            override val message: String = "Failed to save data",
            override val cause: Throwable? = null
        ) : StorageError()
    }

    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError()
}
```

### Domain Models

```kotlin
// domain/model/AuthUser.kt
data class AuthUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val email: String = "",   // /me returns email; login/register response does not
    val token: String
)

// domain/model/AuthState.kt
sealed class AuthState {
    object Unknown : AuthState()                              // initial — storage not yet read
    object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
}
```

### DTOs

The backend's `AuthResponse` now includes `username` (confirmed fix applied to `server/src/main/kotlin/models/AuthModels.kt` and `routing/AuthRoutes.kt`). The `/me` endpoint returns a dedicated `UserResponse` on the server, mapped to `MeResponse` on the client — no `token` field, no empty-string lie.

```kotlin
// data/remote/dto/AuthDtos.kt
@Serializable data class RegisterRequest(
    val email: String? = null,
    val password: String? = null,
    val username: String? = null,
    val displayName: String? = null
)

@Serializable data class LoginRequest(
    val email: String? = null,
    val password: String? = null
)

@Serializable data class AuthResponse(
    val token: String? = null,
    val userId: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val displayName: String? = null
)

// Used exclusively for GET /me — no token field
@Serializable data class MeResponse(
    val userId: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val displayName: String? = null
)

@Serializable data class ErrorResponse(
    val message: String? = null
)
```

> **Backend changes applied:**
> - `AuthResponse` in `models/AuthModels.kt` now has `username: String` between `userId` and `email`.
> - A new `UserResponse(userId, username, email, displayName)` data class was added to `models/AuthModels.kt`.
> - `/register` and `/login` handlers now pass `username` to `AuthResponse(...)`.
> - `/me` handler now queries `UsersTable.username` and returns `UserResponse` instead of a fake `AuthResponse(token = "")`.
> - `AuthUserRow` (private to `AuthRoutes.kt`) now includes `username`.

---

## Key Design Decisions

### 1. `username` in `AuthResponse` — server fix applied

The backend's `AuthResponse` previously omitted `username`. This has been corrected: `models/AuthModels.kt` now includes `username: String` in `AuthResponse`, and all route handlers (`/register`, `/login`, `/google`, `/apple`) now populate it from the database row. The client DTO mirrors this exactly — no fallback derivation needed.

### 2. `/me` uses a dedicated `MeResponse` — no `token = ""` hack

The `/me` endpoint now returns `UserResponse` on the server (mapped to `MeResponse` on the client). `MeResponse` has no `token` field — the client already holds the token it sent in the `Authorization` header. The mapper's `MeResponse.toAuthUser(token: String)` overload receives the stored token as a plain parameter. This is composition, not substitution.

### 3. `isLoggedIn()` removed entirely

`isLoggedIn()` has been removed from `AuthRepository`, `AuthRepositoryImpl`, and the use case layer. The rationale:
- The UI collects `observeAuthState()` — it already has a reactive, accurate answer.
- A network interceptor needs the token string, not a boolean — `tokenStorage.getToken()` is the right call.
- Any action that requires auth returns `AuthError.Unauthorized` if no token is present — that's the correct place to learn it.
- A synchronous `isLoggedIn()` would return `false` during the `Unknown` hydration window, silently lying to callers.

The `_authState` `MutableStateFlow` starts at `AuthState.Unknown`. The `onStart` operator on the returned flow triggers `hydrateFromStorage()` only when the first collector subscribes and the state is still `Unknown`. Subsequent collectors receive the current `StateFlow` value immediately (StateFlow semantics — no re-hydration).

Hydration calls `getCurrentUser()` optimistically if a token exists. If `/me` returns 401, the token is cleared and `Unauthenticated` is emitted. If the network is down, the state stays `Unknown` (transient error — don't flip state).

### 3. HTTP error detection

Ktor throws `ResponseException` for non-2xx responses. The repository catches it and inspects `response.status.value`:

```kotlin
} catch (e: ResponseException) {
    when (e.response.status.value) {
        409 -> AppError.AuthError.UserAlreadyExists().asLeft()
        401 -> AppError.AuthError.InvalidCredentials().asLeft()  // login
        // etc.
    }
}
```

For `/me` specifically, 401 also triggers `tokenStorage.clearToken()` and `_authState.value = AuthState.Unauthenticated`.

### 4. `isLoggedIn()` is not suspend in the interface

The interface declares `fun isLoggedIn(): Boolean` (not suspend). The implementation reads the `StateFlow`'s current value synchronously — it does not call `tokenStorage.getToken()` (which is suspend). This is safe because the `StateFlow` is always up to date after hydration.

### 5. Koin version

`libs.versions.toml` declares `koin = "4.1.1"`. The `module { }` DSL is unchanged between 3.x and 4.x for the patterns used here.

---

## AuthRepositoryImpl — State Machine

```
Initial:  _authState = Unknown

observeAuthState() first collector
  └─ hydrateFromStorage()
       ├─ no token → Unauthenticated
       └─ token present → getCurrentUser()
            ├─ success → Authenticated(user)
            ├─ 401/Unauthorized → clearToken() → Unauthenticated
            └─ network error → (no state change, stays Unknown)

login(email, password)
  ├─ validation fails → Left(ValidationError), no state change
  ├─ success → saveToken() → Authenticated(user) → Right(user)
  ├─ 401 → Left(InvalidCredentials), no state change
  └─ network error → Left(NetworkError), no state change

register(email, password, username, displayName)
  ├─ validation fails → Left(ValidationError), no state change
  ├─ success → saveToken() → Authenticated(user) → Right(user)
  ├─ 409 → Left(UserAlreadyExists), no state change
  └─ network error → Left(NetworkError), no state change

logout()
  └─ clearToken() → Unauthenticated → Right(Unit)
```

---

## Correctness Properties

### Property 1: Validator idempotence

For any string input, calling `validateEmail`, `validatePassword`, `validateUsername`, or `validateDisplayName` twice with the same argument SHALL return identical `Either` values. These are pure functions with no side effects.

### Property 2: AuthMapper round-trip

For any `AuthResponse` where `token`, `userId`, and `displayName` are all non-null and non-blank, `toAuthUser()` SHALL return `Right`. No valid DTO should produce `Left`.

### Property 3: AuthState monotonicity during transient errors

If `_authState` is `Authenticated` and a network call fails with `NoConnection` or `Timeout`, `_authState` SHALL remain `Authenticated`. Transient errors never downgrade auth state.

---

## Error Handling Summary

| Scenario | AppError returned | State change |
|----------|-------------------|--------------|
| Invalid email/password input | `ValidationError.InvalidEmail` / `PasswordTooShort` | None |
| Register — email already exists (409) | `AuthError.UserAlreadyExists` | None |
| Login — wrong credentials (401) | `AuthError.InvalidCredentials` | None |
| `/me` — token expired (401) | `AuthError.TokenExpired` | → Unauthenticated |
| `/me` — user not found (404) | `AuthError.Unauthorized` | None |
| No stored token on `getCurrentUser` | `AuthError.Unauthorized` | None |
| Network unreachable | `NetworkError.NoConnection` | None |
| Request timeout | `NetworkError.Timeout` | None |
| JSON parse failure | `NetworkError.ParseError` | None |
| Storage write failure | `StorageError.WriteFailed` | None |
| Storage read failure | `StorageError.ReadFailed` | None |
| Unexpected exception | `Unknown` | None |

---

## Testing Strategy

Unit tests are out of scope for this task but the design is structured for testability:

- `Validator` is a pure `object` — no dependencies, trivially testable.
- `AuthMapper` is a pure `object` — no dependencies, trivially testable.
- `AuthRepositoryImpl` accepts `AuthApi` and `TokenStorage` as constructor parameters — both can be faked/mocked.
- `AuthApi` accepts `HttpClient` and `baseUrl` — Ktor's `MockEngine` can be used for integration tests.
- `TokenStorage` is an interface — a simple in-memory fake suffices for unit tests.
