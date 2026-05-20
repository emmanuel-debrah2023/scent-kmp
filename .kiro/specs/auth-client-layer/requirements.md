# Requirements Document

## Introduction

This feature builds the Auth Client Layer for the KMP `shared/` module of the Scent fragrance social commerce app. The Ktor backend already exposes Phase 1 JWT auth endpoints (`/register`, `/login`, `/me`). This layer provides the shared business logic — domain models, repository, use cases, local token storage, and Koin DI wiring — that Android (and later iOS) clients consume. The scope stops at the shared module boundary: no ViewModels, Composables, screens, or navigation are included.

The existing `shared/` module contains several files that must be fully replaced because they use `kotlin.Result`, lack the `Either<AppError, T>` pattern, use hardcoded base URLs, or are missing required fields. Two use cases (`AppleAuthUseCase`, `GoogleAuthUseCase`) are out of scope for Phase 1 and must be deleted.

## Glossary

- **AuthApi**: The Ktor HTTP client wrapper that calls the backend auth endpoints.
- **AuthMapper**: The object responsible for converting `AuthResponse` and `MeResponse` DTOs into `AuthUser` domain models.
- **MeResponse**: The DTO returned by the `/me` endpoint — contains `userId`, `username`, `email`, `displayName` but no `token`.
- **AuthRepository**: The domain interface defining all auth operations available to use cases.
- **AuthRepositoryImpl**: The data-layer implementation of `AuthRepository`.
- **AuthState**: A sealed class representing the current authentication state (`Unknown`, `Unauthenticated`, `Authenticated`).
- **AuthUser**: The clean domain model representing an authenticated user.
- **AppError**: The sealed error hierarchy used across the shared module.
- **Either**: The functional type `Either<L, R>` with `Left` and `Right` variants used for all fallible operations.
- **HttpClientFactory**: An `expect`/`actual` factory that creates a platform-specific Ktor `HttpClient`.
- **JsonConfig**: The shared `kotlinx.serialization` `Json` instance configured for lenient parsing.
- **Result**: A type alias `typealias Result<T> = Either<AppError, T>`.
- **SharedModule**: The Koin module that wires all shared dependencies.
- **TokenStorage**: The interface for persisting, reading, and clearing the JWT token on-device.
- **TokenStorageFactory**: An `expect`/`actual` class that creates the platform-specific `TokenStorage` implementation.
- **Validator**: The object that validates user-supplied input fields before network calls are made.

---

## Requirements

### Requirement 1: Either / Result Pattern

**User Story:** As a mobile developer, I want all fallible shared-module operations to return `Either<AppError, T>` instead of throwing exceptions or using `kotlin.Result`, so that error handling is explicit, exhaustive, and composable at every call site.

#### Acceptance Criteria

1. THE `Either` class SHALL be a sealed class with exactly two variants: `Left<out L>` and `Right<out R>`, located at `domain/util/Either.kt` under package `org.scent.project.domain.util`.
2. THE `Either` class SHALL expose `isRight`, `isLeft`, `fold`, `map`, `flatMap`, `onRight`, `onLeft`, `getOrNull`, and `leftOrNull` members.
3. THE `Either` class SHALL provide top-level extension functions `fun <R> R.asRight()` and `fun <L> L.asLeft()` in the same file.
4. THE `Either` class SHALL provide the type alias `typealias Result<T> = Either<AppError, T>` in the same file.
5. WHEN any fallible operation in the shared module is called, THE shared module SHALL return `Result<T>` rather than throwing an exception or returning `kotlin.Result`.
6. THE shared module SHALL NOT use `!!` (non-null assertion) anywhere in production code.
7. THE shared module SHALL NOT use nullable domain models (all domain model fields are non-null unless semantically meaningful).

---

### Requirement 2: AppError Hierarchy

**User Story:** As a mobile developer, I want a typed, exhaustive error hierarchy, so that the UI layer can pattern-match on specific error cases and display appropriate messages without inspecting raw exception messages.

#### Acceptance Criteria

1. THE `AppError` sealed class SHALL be located at `domain/error/AppError.kt` under package `org.scent.project.domain.error`.
2. THE `AppError` sealed class SHALL expose `val message: String` and `val cause: Throwable?` on every variant.
3. THE `AppError.NetworkError` sealed subclass SHALL contain: `NoConnection`, `Timeout`, `ServerError(statusCode: Int)`, and `ParseError(fieldName: String? = null)`.
4. THE `AppError.AuthError` sealed subclass SHALL contain: `InvalidCredentials`, `UserAlreadyExists`, `TokenExpired`, and `Unauthorized`.
5. THE `AppError.ValidationError` sealed subclass SHALL contain: `InvalidEmail`, `PasswordTooShort(minLength: Int = 8)`, `RequiredFieldEmpty(fieldName: String)`, and `InvalidInput(fieldName: String)`.
6. THE `AppError.StorageError` sealed subclass SHALL contain: `ReadFailed` and `WriteFailed`.
7. THE `AppError` sealed class SHALL contain a top-level `Unknown(message: String, cause: Throwable? = null)` variant.

---

### Requirement 3: Input Validation

**User Story:** As a mobile developer, I want input validation to run before any network call, so that the user receives immediate, specific feedback for malformed inputs without consuming network resources.

#### Acceptance Criteria

1. THE `Validator` object SHALL be located at `domain/validation/Validator.kt` under package `org.scent.project.domain.validation`.
2. WHEN `Validator.validateEmail(email)` is called with a string that does not match a standard email regex, THE `Validator` SHALL return `AppError.ValidationError.InvalidEmail.asLeft()`.
3. WHEN `Validator.validateEmail(email)` is called with a valid email string, THE `Validator` SHALL return `email.asRight()`.
4. WHEN `Validator.validatePassword(password)` is called with a string shorter than 8 characters, THE `Validator` SHALL return `AppError.ValidationError.PasswordTooShort(minLength = 8).asLeft()`.
5. WHEN `Validator.validatePassword(password)` is called with a string of 8 or more characters, THE `Validator` SHALL return `password.asRight()`.
6. WHEN `Validator.validateUsername(username)` is called with a string shorter than 3 characters or containing characters other than letters, digits, or underscores, THE `Validator` SHALL return `AppError.ValidationError.InvalidInput(fieldName = "username").asLeft()`.
7. WHEN `Validator.validateUsername(username)` is called with a valid username, THE `Validator` SHALL return `username.asRight()`.
8. WHEN `Validator.validateDisplayName(displayName)` is called with a blank string, THE `Validator` SHALL return `AppError.ValidationError.RequiredFieldEmpty(fieldName = "displayName").asLeft()`.
9. WHEN `Validator.validateDisplayName(displayName)` is called with a string exceeding 100 characters, THE `Validator` SHALL return `AppError.ValidationError.InvalidInput(fieldName = "displayName").asLeft()`.
10. WHEN `Validator.validateDisplayName(displayName)` is called with a non-blank string of 100 characters or fewer, THE `Validator` SHALL return `displayName.asRight()`.
11. FOR ALL valid inputs, calling each `Validator` function twice with the same input SHALL produce the same result (idempotence property).

---

### Requirement 4: Domain Models

**User Story:** As a mobile developer, I want clean, non-null domain models that represent authenticated users and auth state, so that the UI layer never needs to null-check domain objects.

#### Acceptance Criteria

1. THE `AuthUser` data class SHALL be located at `domain/model/AuthUser.kt` under package `org.scent.project.domain.model` and SHALL have non-null fields: `id: Int`, `username: String`, `displayName: String`, `email: String`, `token: String`.
2. THE `AuthState` sealed class SHALL be located at `domain/model/AuthState.kt` under package `org.scent.project.domain.model` and SHALL have exactly three variants: `Unknown`, `Unauthenticated`, and `Authenticated(user: AuthUser)`.
3. THE `AuthUser` data class SHALL NOT have nullable fields.

---

### Requirement 5: Data Transfer Objects

**User Story:** As a mobile developer, I want typed DTOs that exactly match the backend's camelCase JSON contract, so that serialization succeeds without `@SerialName` annotations and without crashing on missing or null fields.

#### Acceptance Criteria

1. THE `AuthDtos.kt` file SHALL be located at `data/remote/dto/AuthDtos.kt` under package `org.scent.project.data.remote.dto`.
2. THE `RegisterRequest` data class SHALL have fields: `email: String`, `password: String`, `username: String`, `displayName: String`.
3. THE `LoginRequest` data class SHALL have fields: `email: String`, `password: String`.
4. THE `AuthResponse` data class SHALL have nullable fields with null defaults: `token: String? = null`, `userId: Int? = null`, `username: String? = null`, `email: String? = null`, `displayName: String? = null`.
5. THE `MeResponse` data class SHALL have nullable fields with null defaults: `userId: Int? = null`, `username: String? = null`, `email: String? = null`, `displayName: String? = null` — no `token` field.
6. THE `ErrorResponse` data class SHALL have a single field: `message: String? = null`.
7. WHEN the backend returns a JSON object with unknown keys, THE `AuthResponse` or `MeResponse` deserializer SHALL ignore those keys without throwing an exception.
8. WHEN the backend returns a null value for a non-nullable Kotlin field, THE deserializer SHALL coerce the value to the field's default rather than throwing an exception.
9. THE `AuthDtos.kt` file SHALL NOT use `@SerialName` annotations for the `token`, `userId`, `username`, `email`, or `displayName` fields, because the backend serializes these fields in camelCase without renaming.

---

### Requirement 6: JSON Configuration

**User Story:** As a mobile developer, I want a shared `Json` instance configured for lenient parsing, so that minor server-side changes (extra fields, null coercion) do not crash the client.

#### Acceptance Criteria

1. THE `JsonConfig` object SHALL be located at `data/remote/JsonConfig.kt` under package `org.scent.project.data.remote`.
2. THE `JsonConfig` object SHALL expose a single `val json: Json` property configured with `isLenient = true`, `ignoreUnknownKeys = true`, `coerceInputValues = true`, and `encodeDefaults = true`.
3. THE `HttpClientFactory` SHALL install `ContentNegotiation` using the `JsonConfig.json` instance.

---

### Requirement 7: HTTP Client Factory

**User Story:** As a mobile developer, I want a platform-specific Ktor `HttpClient` created via `expect`/`actual`, so that Android uses the OkHttp engine and iOS uses the Darwin engine without any shared-module code referencing platform APIs directly.

#### Acceptance Criteria

1. THE `HttpClientFactory.kt` file SHALL declare `expect fun createHttpClient(): HttpClient` in `data/remote/HttpClientFactory.kt` under package `org.scent.project.data.remote`.
2. THE Android actual implementation SHALL use the OkHttp engine and SHALL be located at `androidMain/.../data/remote/HttpClientFactory.android.kt`.
3. THE iOS actual implementation SHALL use the Darwin engine and SHALL be located at `iosMain/.../data/remote/HttpClientFactory.ios.kt`.
4. WHEN `createHttpClient()` is called on either platform, THE `HttpClientFactory` SHALL install `ContentNegotiation` with `JsonConfig.json`, `HttpTimeout` with a 30-second request timeout, and `Logging`.
5. THE `iosMain` source set SHALL declare a dependency on `ktor-client-darwin` in `shared/build.gradle.kts` (currently commented out and must be uncommented).

---

### Requirement 8: Token Storage

**User Story:** As a mobile developer, I want a platform-specific token storage implementation behind a common interface, so that the JWT is persisted securely on each platform without the shared module depending on platform APIs.

#### Acceptance Criteria

1. THE `TokenStorage` interface SHALL be located at `data/local/TokenStorage.kt` under package `org.scent.project.data.local` and SHALL declare: `suspend fun saveToken(token: String): Result<Unit>`, `suspend fun getToken(): Result<String?>`, and `suspend fun clearToken(): Result<Unit>`.
2. THE `expect class TokenStorageFactory` SHALL be declared in the same file as `TokenStorage`.
3. THE Android actual `TokenStorageFactory` SHALL accept a `context: Context` parameter, use `DataStore<Preferences>` with file name `"scent_auth_prefs"`, and store the token under key `"auth_token"`.
4. THE Android actual `TokenStorageFactory` SHALL be located at `androidMain/.../data/local/TokenStorageFactory.android.kt`.
5. THE iOS actual `TokenStorageFactory` SHALL use `NSUserDefaults` for token storage and SHALL include a `// TODO: migrate to Keychain` comment.
6. THE iOS actual `TokenStorageFactory` SHALL be located at `iosMain/.../data/local/TokenStorageFactory.ios.kt`.
7. WHEN `saveToken` is called and the write succeeds, THE `TokenStorage` SHALL return `Unit.asRight()`.
8. IF a storage write operation fails, THEN THE `TokenStorage` SHALL return `AppError.StorageError.WriteFailed.asLeft()`.
9. IF a storage read operation fails, THEN THE `TokenStorage` SHALL return `AppError.StorageError.ReadFailed.asLeft()`.
10. WHEN `getToken` is called and no token has been saved, THE `TokenStorage` SHALL return `null.asRight()` (not an error).

---

### Requirement 9: Auth API Client

**User Story:** As a mobile developer, I want a typed `AuthApi` class that calls the backend auth endpoints with explicit Bearer token headers, so that authentication requests are strongly typed and the Ktor Auth plugin is not required.

#### Acceptance Criteria

1. THE `AuthApi` class SHALL be located at `data/remote/api/AuthApi.kt` under package `org.scent.project.data.remote.api`.
2. THE `AuthApi` constructor SHALL accept `httpClient: HttpClient` and `baseUrl: String` as parameters.
3. THE `AuthApi` SHALL expose: `suspend fun register(request: RegisterRequest): AuthResponse`, `suspend fun login(request: LoginRequest): AuthResponse`, and `suspend fun getCurrentUser(token: String): MeResponse`.
4. WHEN `getCurrentUser` is called, THE `AuthApi` SHALL set the `Authorization` header to `"Bearer $token"` explicitly, without using the Ktor Auth plugin.
5. THE `AuthApi` SHALL NOT contain methods for Google or Apple auth (Phase 2/3 scope).
6. THE `AuthApi` SHALL NOT hardcode a base URL; the base URL SHALL be injected via the constructor.

---

### Requirement 10: Auth Mapper

**User Story:** As a mobile developer, I want a mapper that converts `AuthResponse` DTOs into `AuthUser` domain models with explicit validation, so that the domain layer never receives a partially-populated user object.

#### Acceptance Criteria

1. THE `AuthMapper` object SHALL be located at `data/mapper/AuthMapper.kt` under package `org.scent.project.data.mapper`.
2. THE `AuthMapper` SHALL expose `fun AuthResponse.toAuthUser(): Result<AuthUser>` that validates `token`, `userId`, `username`, and `displayName` are non-null and non-blank, returning `AppError.NetworkError.ParseError` if any required field is absent.
3. THE `AuthMapper` SHALL expose `fun MeResponse.toAuthUser(token: String): Result<AuthUser>` for mapping `/me` responses — `MeResponse` has no `token` field, so the stored token is passed in as a parameter.
4. WHEN `AuthResponse.toAuthUser()` is called with a valid `AuthResponse`, THE `AuthMapper` SHALL return an `AuthUser` with all fields populated from the DTO.
5. WHEN `MeResponse.toAuthUser(token)` is called, THE `AuthMapper` SHALL use the supplied `token` parameter as the `token` field of the resulting `AuthUser`.
6. FOR ALL valid `AuthResponse` objects, `toAuthUser()` SHALL return `Right` (round-trip property: a valid DTO always maps to a valid domain model).

---

### Requirement 11: Auth Repository Interface

**User Story:** As a mobile developer, I want a clean repository interface that exposes all Phase 1 auth operations with correct return types, so that use cases depend only on the abstraction and not on any data-layer implementation detail.

#### Acceptance Criteria

1. THE `AuthRepository` interface SHALL be located at `domain/repository/AuthRepository.kt` under package `org.scent.project.domain.repository`.
2. THE `AuthRepository` interface SHALL declare: `fun observeAuthState(): Flow<AuthState>` (not suspend), `suspend fun register(email: String, password: String, username: String, displayName: String): Result<AuthUser>`, `suspend fun login(email: String, password: String): Result<AuthUser>`, `suspend fun getCurrentUser(): Result<AuthUser>`, and `suspend fun logout(): Result<Unit>`.
3. THE `AuthRepository` interface SHALL NOT declare methods for Google or Apple auth.
4. THE `AuthRepository` interface SHALL include commented-out stubs for Phase 2/3 methods.

---

### Requirement 12: Auth Repository Implementation

**User Story:** As a mobile developer, I want the repository implementation to validate inputs, call the API, map errors to typed `AppError` variants, and keep the auth state `Flow` up to date, so that all callers receive consistent, predictable results.

#### Acceptance Criteria

1. THE `AuthRepositoryImpl` class SHALL be located at `data/repository/AuthRepositoryImpl.kt` under package `org.scent.project.data.repository`.
2. THE `AuthRepositoryImpl` constructor SHALL accept `api: AuthApi`, `tokenStorage: TokenStorage`, and `validator: Validator` (or equivalent) as parameters.
3. WHEN `register` is called, THE `AuthRepositoryImpl` SHALL validate email, password, username, and displayName via `Validator` before making any network call, returning the first validation error encountered.
4. WHEN `login` is called, THE `AuthRepositoryImpl` SHALL validate email and password via `Validator` before making any network call, and SHALL always make the network call after validation passes.
5. WHEN the backend returns HTTP 409 on register, THE `AuthRepositoryImpl` SHALL return `AppError.AuthError.UserAlreadyExists.asLeft()`.
6. WHEN the backend returns HTTP 401 on login, THE `AuthRepositoryImpl` SHALL return `AppError.AuthError.InvalidCredentials.asLeft()`.
7. WHEN the backend returns HTTP 401 on `/me`, THE `AuthRepositoryImpl` SHALL return `AppError.AuthError.TokenExpired.asLeft()`, clear the stored token, and emit `AuthState.Unauthenticated` on the auth state flow.
8. WHEN the backend returns HTTP 404 on `/me`, THE `AuthRepositoryImpl` SHALL return `AppError.AuthError.Unauthorized.asLeft()`.
9. WHEN a network call throws an `IOException`, THE `AuthRepositoryImpl` SHALL return `AppError.NetworkError.NoConnection.asLeft()`.
10. WHEN a network call times out, THE `AuthRepositoryImpl` SHALL return `AppError.NetworkError.Timeout.asLeft()`.
11. WHEN a network call throws a `SerializationException`, THE `AuthRepositoryImpl` SHALL return `AppError.NetworkError.ParseError().asLeft()`.
12. WHEN any other unexpected exception is thrown, THE `AuthRepositoryImpl` SHALL return `AppError.Unknown(message, cause).asLeft()`.
13. WHEN `getCurrentUser` is called and no token is stored, THE `AuthRepositoryImpl` SHALL return `AppError.AuthError.Unauthorized.asLeft()` without making a network call.
14. WHEN `logout` is called, THE `AuthRepositoryImpl` SHALL call `tokenStorage.clearToken()`, emit `AuthState.Unauthenticated`, and return `Unit.asRight()`.
15. THE `AuthRepositoryImpl` SHALL maintain a `MutableStateFlow<AuthState>` initialised to `AuthState.Unknown`.
16. WHEN `observeAuthState()` is called, THE `AuthRepositoryImpl` SHALL use `onStart` to hydrate the state from token storage on the first collector, emitting `AuthState.Authenticated` if a token is present or `AuthState.Unauthenticated` if not.
17. WHEN `login`, `register`, or `getCurrentUser` succeeds, THE `AuthRepositoryImpl` SHALL emit `AuthState.Authenticated(user)` on the auth state flow.
18. WHEN a transient error occurs (network, timeout, parse), THE `AuthRepositoryImpl` SHALL NOT change the current auth state.

---

### Requirement 13: Use Cases

**User Story:** As a mobile developer, I want thin use-case classes that delegate to the repository, so that ViewModels depend on single-responsibility interactors rather than the full repository interface.

#### Acceptance Criteria

1. THE `LoginUseCase` class SHALL be located at `domain/usecase/LoginUseCase.kt` and SHALL expose `suspend operator fun invoke(email: String, password: String): Result<AuthUser>`.
2. THE `RegisterUseCase` class SHALL be located at `domain/usecase/RegisterUseCase.kt` and SHALL expose `suspend operator fun invoke(email: String, password: String, username: String, displayName: String): Result<AuthUser>`.
3. THE `GetCurrentUserUseCase` class SHALL be located at `domain/usecase/GetCurrentUserUseCase.kt` and SHALL expose `suspend operator fun invoke(): Result<AuthUser>`.
4. THE `LogoutUseCase` class SHALL be located at `domain/usecase/LogoutUseCase.kt` and SHALL expose `suspend operator fun invoke(): Result<Unit>`.
5. THE `ObserveAuthStateUseCase` class SHALL be located at `domain/usecase/ObserveAuthStateUseCase.kt` and SHALL expose `operator fun invoke(): Flow<AuthState>` (not suspend).
6. THE `AppleAuthUseCase.kt`, `GoogleAuthUseCase.kt`, and `IsLoggedInUseCase.kt` files SHALL be deleted from the shared module (or not created — `IsLoggedInUseCase` is redundant given `observeAuthState()`).
7. WHEN any use case `invoke` is called, THE use case SHALL delegate directly to the corresponding `AuthRepository` method without adding additional logic.

---

### Requirement 14: Koin Dependency Injection

**User Story:** As a mobile developer, I want a single Koin module that wires all shared auth dependencies, so that platform-specific app modules only need to call one function to set up the shared layer.

#### Acceptance Criteria

1. THE `SharedModule.kt` file SHALL be located at `di/SharedModule.kt` under package `org.scent.project.di`.
2. THE `SharedModule` SHALL expose `fun sharedModule(baseUrl: String, tokenStorageFactory: TokenStorageFactory) = module { ... }`.
3. THE `SharedModule` SHALL register as `single`: `HttpClient` (via `createHttpClient()`), `TokenStorage` (via `tokenStorageFactory`), `AuthApi` (with injected `HttpClient` and `baseUrl`), and `AuthRepository` bound to `AuthRepositoryImpl`.
4. THE `SharedModule` SHALL register as `factory`: `LoginUseCase`, `RegisterUseCase`, `GetCurrentUserUseCase`, `LogoutUseCase`, and `ObserveAuthStateUseCase`.
5. WHEN `sharedModule` is called with the same `baseUrl` and `tokenStorageFactory`, THE Koin module SHALL produce the same singleton instances for `single` bindings on repeated `get()` calls (idempotence of singleton resolution).

---

### Requirement 15: Gradle Build Configuration

**User Story:** As a mobile developer, I want the `shared/build.gradle.kts` to declare all required dependencies so that the module compiles on both Android and iOS targets without manual intervention.

#### Acceptance Criteria

1. THE `shared/build.gradle.kts` SHALL declare `ktor-client-logging` in `commonMain.dependencies` (it is currently missing).
2. THE `shared/build.gradle.kts` SHALL uncomment and declare `ktor-client-darwin` in `iosMain.dependencies`.
3. THE `shared/build.gradle.kts` SHALL retain all existing dependencies: `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `kotlinx-coroutines-core`, `kotlinx-serialization-json`, `koin-core`, `ktor-client-okhttp` (androidMain), and `androidx-datastore-preferences` (androidMain).
4. THE `shared/build.gradle.kts` SHALL NOT add `ktor-client-auth` as a required dependency for Phase 1 (Bearer tokens are set manually via headers).
5. WHEN the shared module is built for the Android target, THE build SHALL succeed without unresolved dependency errors.
6. WHEN the shared module is built for the iOS target, THE build SHALL succeed without unresolved dependency errors.

---

### Requirement 16: File Replacement and Deletion

**User Story:** As a mobile developer, I want all incorrect existing files replaced and out-of-scope files deleted, so that the codebase has no conflicting implementations that could cause compilation errors or confuse future contributors.

#### Acceptance Criteria

1. THE following files SHALL be fully replaced with correct implementations: `data/local/TokenStorage.kt`, `data/remote/api/AuthApi.kt`, `data/remote/dto/AuthDtos.kt`, `data/repository/AuthRepositoryImpl.kt`, `domain/model/AuthUser.kt`, `domain/repository/AuthRepository.kt`, `domain/usecase/LoginUseCase.kt`, `domain/usecase/RegisterUseCase.kt`, `domain/usecase/GetCurrentUserUseCase.kt`, and `domain/usecase/LogoutUseCase.kt`.
2. THE `domain/usecase/AppleAuthUseCase.kt`, `domain/usecase/GoogleAuthUseCase.kt`, and `domain/usecase/IsLoggedInUseCase.kt` files SHALL be deleted (or not created).
3. WHEN the replacement files are in place, THE shared module SHALL compile without referencing `kotlin.Result` in any auth-related file.
4. WHEN the replacement files are in place, THE shared module SHALL compile without any reference to `loginWithGoogle`, `loginWithApple`, or `isLoggedIn` in the `AuthRepository` interface.
