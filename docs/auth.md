# Scent Project — Auth Strategy

## Project Context

**App:** Fragrance marketplace with short-form vertical video feed (TikTok-style)
**Stack:** Kotlin Multiplatform (KMP), Ktor backend, Exposed ORM, PostgreSQL
**Deployment:** Render free tier (backend) + Supabase (Postgres)
**Clients:** Android and iOS via shared KMP module
**Architecture:** Use-case / interactor layer in shared module

---

## Auth Roadmap

```
Phase 1 (current) → JWT email/password
Phase 2 (next)    → Google OAuth
Phase 3           → Apple Sign-In (required for iOS App Store)
```

---

## Phase 1 — JWT (Email/Password)

### Why JWT for this stack
- Render free tier has no persistent memory between cold starts — sessions would be lost
- KMP clients (Android/iOS) work best with token-based auth — no cookie handling needed
- Stateless tokens require zero additional infrastructure (no Redis, no session store)

### Token Lifecycle

```
POST /api/v1/auth/register  →  BCrypt hash password, insert user, return access token
POST /api/v1/auth/login     →  Verify BCrypt hash, return access token
GET  /api/v1/auth/me        →  Protected route, decode userId from token, return profile
```

**Token expiry:** 24 hours
**Token payload:** `userId` (Int), `issuer`, `audience`
**Algorithm:** HMAC256

### Key Files

| File | Purpose |
|---|---|
| `plugins/Security.kt` | JWT plugin install, token verifier, `generateToken()` helper |
| `routes/AuthRoutes.kt` | Register, login, /me routes |
| `models/AuthModels.kt` | `RegisterRequest`, `LoginRequest`, `AuthResponse`, `ErrorResponse` |
| `application.conf` | JWT secret, issuer, audience, database config via env vars |
| `Database.kt` | `initDatabase()` — Exposed `SchemaUtils.createMissingTablesAndColumns()` |

### application.conf Structure

```hocon
jwt {
    secret   = ${JWT_SECRET}
    issuer   = "fragrances-app"
    audience = "fragrances-users"
    realm    = "fragrances"
}

database {
    url      = ${DATABASE_URL}
    user     = ${DATABASE_USER}
    password = ${DATABASE_PASSWORD}
    driver   = "org.postgresql.Driver"
}
```

### KMP Shared Module Structure

```
shared/src/commonMain/kotlin/
├── data/
│   ├── remote/
│   │   ├── api/AuthApi.kt              ← Ktor client calls
│   │   └── dto/AuthDtos.kt             ← Request/Response models
│   ├── local/
│   │   └── TokenStorage.kt             ← interface (expect/actual)
│   └── repository/
│       └── AuthRepositoryImpl.kt       ← wires API + storage
├── domain/
│   ├── model/AuthUser.kt               ← clean domain model
│   ├── repository/AuthRepository.kt    ← interface
│   └── usecase/
│       ├── LoginUseCase.kt
│       ├── RegisterUseCase.kt
│       └── GetCurrentUserUseCase.kt
shared/src/androidMain/kotlin/
└── data/local/TokenStorageImpl.kt      ← DataStore implementation
```

### Auth Flow (KMP Client Side)

```
1. User registers or logs in via ViewModel → UseCase → Repository → AuthApi
2. Server returns AuthResponse { token, userId, username, displayName }
3. Token saved to DataStore (Android) / Keychain (iOS) via TokenStorage
4. Every subsequent request sends:
   Authorization: Bearer <token>
5. Protected Ktor routes use authenticate("auth-jwt") block to verify
```

### Protected Route Pattern (Ktor)

```kotlin
authenticate("auth-jwt") {
    get("/some-protected-route") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("userId").asInt()
        // scope all DB queries to this userId
    }
}
```

### Local Development Note

Android emulator cannot reach `localhost` — use `10.0.2.2:8080` instead:

```kotlin
// AuthApi.kt — local dev
private val baseUrl = "http://10.0.2.2:8080"

// Production
private val baseUrl = "https://your-app.onrender.com"
```

### Phase 1 Known Gaps

| Gap | Fix |
|---|---|
| No refresh token | Add `refresh_tokens` table, issue short + long lived token pair |
| Token can't be invalidated early | Add `token_blocklist` table for logout/ban |
| No rate limiting on login | Add Ktor rate limiting plugin to prevent brute force |
| Password reset | Needs email provider (e.g. Resend) — not yet implemented |

---

## Phase 2 — Google OAuth

### Overview

The mobile client handles Google sign-in natively, gets an `idToken` back from Google, then sends it to your Ktor backend to verify and exchange for your own app JWT.

### Backend Changes

**1. Add dependency to `server/build.gradle.kts`:**
```kotlin
implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
```

**2. New Ktor route `POST /api/v1/auth/google`:**
```kotlin
post("/google") {
    val body = call.receive<GoogleAuthRequest>()

    val verifier = GoogleIdTokenVerifier.Builder(
        NetHttpTransport(), GsonFactory.getDefaultInstance()
    )
        .setAudience(listOf(System.getenv("GOOGLE_CLIENT_ID")))
        .build()

    val googleToken = verifier.verify(body.idToken)
        ?: return@post call.respond(
            HttpStatusCode.Unauthorized,
            ErrorResponse("Invalid Google token")
        )

    val payload  = googleToken.payload
    val googleId = payload.subject
    val email    = payload.email
    val name     = payload["name"] as? String ?: "User"

    // Upsert — create on first login, fetch on return
    val userId = transaction {
        val existing = Users.select { Users.googleId eq googleId }.singleOrNull()
        existing?.get(Users.id)?.value ?: Users.insertAndGetId {
            it[Users.googleId]    = googleId
            it[Users.email]       = email
            it[Users.displayName] = name
            it[Users.username]    = email.substringBefore("@")
            it[Users.createdAt]   = LocalDateTime.now()
        }.value
    }

    val token = generateToken(userId, application.environment.config)
    call.respond(HttpStatusCode.OK, AuthResponse(token, userId, email, name))
}
```

**3. Add `googleId` to Users schema:**
```kotlin
val googleId     = varchar("google_id", 255).nullable().uniqueIndex()
val passwordHash = varchar("password_hash", 255).nullable() // nullable for OAuth users
```

### Android Changes

**1. Add to `androidApp/build.gradle.kts`:**
```kotlin
implementation("com.google.android.gms:play-services-auth:21.2.0")
```

**2. Trigger sign-in and pass idToken to shared ViewModel:**
```kotlin
val signInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(
    GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken("YOUR_WEB_CLIENT_ID") // from Google Cloud Console
    .requestEmail()
    .build()
)

// In onActivityResult:
val idToken = GoogleSignIn.getSignedInAccountFromIntent(data).result.idToken
authViewModel.loginWithGoogle(idToken!!)
```

**3. New use case in shared module:**
```kotlin
class GoogleAuthUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<AuthUser> =
        repository.loginWithGoogle(idToken)
}
```

### Environment Variables to Add (Render)

```
GOOGLE_CLIENT_ID = your-web-client-id.apps.googleusercontent.com
```

---

## Phase 3 — Apple Sign-In

### Why Apple is Non-Negotiable for iOS

Apple's App Store policy requires that **any app offering third-party login must also offer Sign in with Apple**. Skipping this will result in App Store rejection.

### How Apple Auth Differs from Google

- Apple tokens are verified against Apple's public JWKS endpoint (not a simple HTTP call)
- Apple only returns the user's name and email **on the very first sign-in** — store them immediately or they are gone
- The `sub` field (Apple user ID) is the only stable identifier across sessions

### Backend Changes

**1. Add dependency to `server/build.gradle.kts`:**
```kotlin
implementation("com.auth0:jwks-rsa:0.22.1")
implementation("com.auth0:java-jwt:4.4.0")
```

**2. New Ktor route `POST /api/v1/auth/apple`:**
```kotlin
post("/apple") {
    val body = call.receive<AppleAuthRequest>()

    // Fetch Apple's public keys and verify the identity token
    val jwkProvider = UrlJwkProvider(URL("https://appleid.apple.com/auth/keys"))
    val decodedJwt  = JWT.decode(body.identityToken)
    val jwk         = jwkProvider.get(decodedJwt.keyId)
    val algorithm   = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)

    val verified = try {
        JWT.require(algorithm)
            .withIssuer("https://appleid.apple.com")
            .withAudience(System.getenv("APPLE_BUNDLE_ID"))
            .build()
            .verify(body.identityToken)
    } catch (e: Exception) {
        return@post call.respond(
            HttpStatusCode.Unauthorized,
            ErrorResponse("Invalid Apple token")
        )
    }

    val appleId   = verified.subject
    val email     = body.email     // only on first sign-in — save it
    val givenName = body.givenName // only on first sign-in — save it

    val userId = transaction {
        val existing = Users.select { Users.appleId eq appleId }.singleOrNull()
        existing?.get(Users.id)?.value ?: Users.insertAndGetId {
            it[Users.appleId]     = appleId
            it[Users.email]       = email ?: "$appleId@privaterelay.appleid.com"
            it[Users.displayName] = givenName ?: "Scent User"
            it[Users.username]    = appleId.take(20)
            it[Users.createdAt]   = LocalDateTime.now()
        }.value
    }

    val token = generateToken(userId, application.environment.config)
    call.respond(HttpStatusCode.OK, AuthResponse(token, userId, email ?: "", givenName ?: ""))
}
```

**3. Add `appleId` to Users schema:**
```kotlin
val appleId = varchar("apple_id", 255).nullable().uniqueIndex()
```

### iOS Changes

Apple Sign-In is handled in the native `iosApp` layer — not in shared KMP code.

**1. Enable capability in Xcode:**
```
Your Target → Signing & Capabilities → + Capability → Sign in with Apple
```

**2. SwiftUI implementation:**
```swift
import AuthenticationServices

SignInWithAppleButton(.signIn) { request in
    request.requestedScopes = [.fullName, .email]
} onCompletion: { result in
    guard case .success(let auth) = result,
          let credential  = auth.credential as? ASAuthorizationAppleIDCredential,
          let tokenData   = credential.identityToken,
          let identityToken = String(data: tokenData, encoding: .utf8)
    else { return }

    // Pass to shared KMP ViewModel
    AuthViewModel().loginWithApple(
        identityToken: identityToken,
        email: credential.email,
        givenName: credential.fullName?.givenName
    )
}
```

**3. New use case in shared module:**
```kotlin
class AppleAuthUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        identityToken: String,
        email: String?,
        givenName: String?
    ): Result<AuthUser> = repository.loginWithApple(identityToken, email, givenName)
}
```

### Environment Variables to Add (Render)

```
APPLE_BUNDLE_ID = com.yourcompany.scentapp
```

---

## Auth Provider Comparison

| | JWT (email/password) | Google OAuth | Apple Sign-In |
|---|---|---|---|
| **Complexity** | Low | Medium | High |
| **iOS App Store** | ✅ | ⚠️ needs Apple too | ✅ required |
| **User friction** | High (form fill) | Low (one tap) | Low (one tap) |
| **Email access** | Always | Always | Hidden relay option |
| **Backend work** | Low | Medium | High (JWKS verify) |
| **Render free tier** | ✅ | ✅ | ✅ |

---

## Unified Users Schema (All 3 Phases)

```kotlin
object Users : IntIdTable("users") {
    val username      = varchar("username", 50).uniqueIndex()
    val email         = varchar("email", 100).uniqueIndex()
    val passwordHash  = varchar("password_hash", 255).nullable() // null for OAuth users
    val googleId      = varchar("google_id", 255).nullable().uniqueIndex()
    val appleId       = varchar("apple_id", 255).nullable().uniqueIndex()
    val displayName   = varchar("display_name", 100)
    val avatarUrl     = varchar("avatar_url", 255).nullable()
    val bio           = text("bio").nullable()
    val isSeller      = bool("is_seller").default(false)
    val followerCount = integer("follower_count").default(0)
    val createdAt     = datetime("created_at")
}
```

---

## Dependencies Summary

### server/build.gradle.kts
```kotlin
implementation("io.ktor:ktor-server-auth:2.3.12")
implementation("io.ktor:ktor-server-auth-jwt:2.3.12")
implementation("org.mindrot:jbcrypt:0.4")
implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0") // Phase 2
implementation("com.auth0:jwks-rsa:0.22.1")                              // Phase 3
implementation("com.auth0:java-jwt:4.4.0")                               // Phase 3
```

### shared/build.gradle.kts
```kotlin
// commonMain
implementation("io.ktor:ktor-client-core:2.3.12")
implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

// androidMain
implementation("io.ktor:ktor-client-okhttp:2.3.12")
implementation("androidx.datastore:datastore-preferences:1.1.1")

// iosMain
implementation("io.ktor:ktor-client-darwin:2.3.12")
```

### androidApp/build.gradle.kts
```kotlin
implementation("com.google.android.gms:play-services-auth:21.2.0") // Phase 2
```

---

## Render Environment Variables (All Phases)

```
JWT_SECRET        = your-super-secret-key
DATABASE_URL      = jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require
DATABASE_USER     = postgres
DATABASE_PASSWORD = your-supabase-password
GOOGLE_CLIENT_ID  = your-web-client-id.apps.googleusercontent.com   # Phase 2
APPLE_BUNDLE_ID   = com.yourcompany.scentapp                         # Phase 3
```
