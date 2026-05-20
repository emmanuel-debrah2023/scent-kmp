# Implementation Tasks

## Task Overview
Build the Phase 1 Auth Client Layer for the KMP `shared/` module. All files go under `shared/src/` with package root `org.scent.project`. Existing incorrect files are replaced; out-of-scope use cases are deleted.

- [x] 1. Foundation — Either, AppError, Validator, domain models
  - [x] 1.1 Create `shared/src/commonMain/kotlin/org/scent/project/domain/util/Either.kt` with sealed `Either<L,R>`, `asLeft()`, `asRight()`, and `typealias Result<T> = Either<AppError, T>`
  - [x] 1.2 Create `shared/src/commonMain/kotlin/org/scent/project/domain/error/AppError.kt` with the full sealed hierarchy: `NetworkError` (NoConnection, Timeout, ServerError, ParseError), `AuthError` (InvalidCredentials, UserAlreadyExists, TokenExpired, Unauthorized), `ValidationError` (InvalidEmail, PasswordTooShort, RequiredFieldEmpty, InvalidInput), `StorageError` (ReadFailed, WriteFailed), `Unknown`
  - [x] 1.3 Create `shared/src/commonMain/kotlin/org/scent/project/domain/validation/Validator.kt` with `validateEmail`, `validatePassword` (min 8), `validateUsername` (min 3, alphanumeric + underscore), `validateDisplayName` (non-blank, max 100)
  - [x] 1.4 Replace `shared/src/commonMain/kotlin/org/scent/project/domain/model/AuthUser.kt` — non-null fields: `id: Int`, `username: String`, `displayName: String`, `email: String = ""`, `token: String`
  - [x] 1.5 Create `shared/src/commonMain/kotlin/org/scent/project/domain/model/AuthState.kt` — sealed class with `Unknown`, `Unauthenticated`, `Authenticated(user: AuthUser)`
  - Requires: nothing
  - Validates: Requirements 1, 2, 3, 4

- [x] 2. DTOs and JSON config
  - [x] 2.1 Replace `shared/src/commonMain/kotlin/org/scent/project/data/remote/dto/AuthDtos.kt` — all fields nullable with `= null` defaults: `RegisterRequest`, `LoginRequest`, `AuthResponse` (token, userId, email, displayName), `ErrorResponse` (message)
  - [x] 2.2 Create `shared/src/commonMain/kotlin/org/scent/project/data/remote/JsonConfig.kt` — `val json: Json` with `isLenient=true`, `ignoreUnknownKeys=true`, `coerceInputValues=true`, `encodeDefaults=true`
  - Requires: Task 1
  - Validates: Requirements 5, 6

- [x] 3. HTTP client factory (expect/actual)
  - [x] 3.1 Create `shared/src/commonMain/kotlin/org/scent/project/data/remote/HttpClientFactory.kt` — `expect fun createHttpClient(): HttpClient`
  - [x] 3.2 Create `shared/src/androidMain/kotlin/org/scent/project/data/remote/HttpClientFactory.android.kt` — `actual fun createHttpClient()` using OkHttp engine, installs ContentNegotiation (JsonConfig.json), HttpTimeout (30s), Logging
  - [x] 3.3 Create `shared/src/iosMain/kotlin/org/scent/project/data/remote/HttpClientFactory.ios.kt` — `actual fun createHttpClient()` using Darwin engine, same plugins
  - [x] 3.4 Update `shared/build.gradle.kts` — add `ktor-client-logging` to commonMain, uncomment `ktor-client-darwin` in iosMain
  - Requires: Task 2
  - Validates: Requirements 7, 15

- [x] 4. Token storage (expect/actual)
  - [x] 4.1 Replace `shared/src/commonMain/kotlin/org/scent/project/data/local/TokenStorage.kt` — interface with `saveToken(): Result<Unit>`, `getToken(): Result<String?>`, `clearToken(): Result<Unit>`; add `expect class TokenStorageFactory` with `fun create(): TokenStorage`
  - [x] 4.2 Create `shared/src/androidMain/kotlin/org/scent/project/data/local/TokenStorageFactory.android.kt` — `actual class TokenStorageFactory(val context: Context)`, DataStore-backed, file `"scent_auth_prefs"`, key `"auth_token"`, wraps I/O in try/catch → StorageError
  - [x] 4.3 Create `shared/src/iosMain/kotlin/org/scent/project/data/local/TokenStorageFactory.ios.kt` — `actual class TokenStorageFactory()`, NSUserDefaults-backed, `// TODO: migrate to Keychain before production` comment at top
  - Requires: Task 1
  - Validates: Requirement 8

- [x] 5. AuthApi
  - [x] 5.1 Replace `shared/src/commonMain/kotlin/org/scent/project/data/remote/api/AuthApi.kt` — constructor takes `httpClient: HttpClient, baseUrl: String`; methods: `register(RegisterRequest): AuthResponse`, `login(LoginRequest): AuthResponse`, `getCurrentUser(token: String): AuthResponse` (explicit `Authorization: Bearer $token` header); no Google/Apple methods
  - Requires: Tasks 2, 3
  - Validates: Requirement 9

- [x] 6. AuthMapper
  - [x] 6.1 Create `shared/src/commonMain/kotlin/org/scent/project/data/mapper/AuthMapper.kt` — `object AuthMapper` with `fun AuthResponse.toAuthUser(): Result<AuthUser>` (validates token, userId, displayName non-null/non-blank; derives username from email) and `fun AuthResponse.toAuthUser(preserveToken: String): Result<AuthUser>` (for /me responses)
  - Requires: Tasks 1, 2
  - Validates: Requirement 10

- [x] 7. AuthRepository interface
  - [x] 7.1 Replace `shared/src/commonMain/kotlin/org/scent/project/domain/repository/AuthRepository.kt` — `fun observeAuthState(): Flow<AuthState>` (not suspend), `suspend fun register/login/getCurrentUser/logout` returning `Result<T>`, `fun isLoggedIn(): Boolean`; Phase 2/3 stubs commented out
  - Requires: Tasks 1
  - Validates: Requirement 11

- [x] 8. AuthRepositoryImpl
  - [x] 8.1 Replace `shared/src/commonMain/kotlin/org/scent/project/data/repository/AuthRepositoryImpl.kt` — constructor takes `api: AuthApi, tokenStorage: TokenStorage`; `_authState = MutableStateFlow(AuthState.Unknown)`; `observeAuthState()` uses `onStart` to hydrate; validates via Validator before network calls; maps HTTP errors (409→UserAlreadyExists, 401 login→InvalidCredentials, 401 /me→TokenExpired+clearToken+Unauthenticated, 404 /me→Unauthorized); catches IOException→NoConnection, timeout→Timeout, SerializationException→ParseError, Exception→Unknown; transient errors do not change `_authState`
  - Requires: Tasks 1, 4, 5, 6, 7
  - Validates: Requirement 12

- [x] 9. Use cases
  - [x] 9.1 Replace `shared/src/commonMain/kotlin/org/scent/project/domain/usecase/LoginUseCase.kt` — `suspend operator fun invoke(email, password): Result<AuthUser>`
  - [x] 9.2 Replace `shared/src/commonMain/kotlin/org/scent/project/domain/usecase/RegisterUseCase.kt` — `suspend operator fun invoke(email, password, username, displayName): Result<AuthUser>`
  - [x] 9.3 Replace `shared/src/commonMain/kotlin/org/scent/project/domain/usecase/GetCurrentUserUseCase.kt` — `suspend operator fun invoke(): Result<AuthUser>`
  - [x] 9.4 Replace `shared/src/commonMain/kotlin/org/scent/project/domain/usecase/LogoutUseCase.kt` — `suspend operator fun invoke(): Result<Unit>`
  - [x] 9.5 Create `shared/src/commonMain/kotlin/org/scent/project/domain/usecase/IsLoggedInUseCase.kt` — `suspend operator fun invoke(): Boolean`
  - [x] 9.6 Create `shared/src/commonMain/kotlin/org/scent/project/domain/usecase/ObserveAuthStateUseCase.kt` — `operator fun invoke(): Flow<AuthState>` (not suspend)
  - [x] 9.7 Delete `shared/src/commonMain/kotlin/org/scent/project/domain/usecase/AppleAuthUseCase.kt`
  - [x] 9.8 Delete `shared/src/commonMain/kotlin/org/scent/project/domain/usecase/GoogleAuthUseCase.kt`
  - Requires: Tasks 1, 7
  - Validates: Requirements 13, 16

- [x] 10. Koin SharedModule
  - [x] 10.1 Create `shared/src/commonMain/kotlin/org/scent/project/di/SharedModule.kt` — `fun sharedModule(baseUrl: String, tokenStorageFactory: TokenStorageFactory) = module { ... }` with singles for HttpClient, TokenStorage, AuthApi, AuthRepository and factories for all 6 use cases
  - Requires: Tasks 3, 4, 5, 8, 9
  - Validates: Requirement 14
