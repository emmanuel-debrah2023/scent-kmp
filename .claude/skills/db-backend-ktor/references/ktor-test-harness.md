# Ktor Test Harness — testApplication, H2, JWT Route Tests

Automated, CI-able server tests that don't need a running Postgres or curl.
Complements (doesn't replace) the smoke scripts: `testApplication` tests catch
logic regressions; smoke scripts catch env/deploy/config issues.

## Dependencies (`server/build.gradle.kts`)

Already wired via the version catalog — don't re-add:

```kotlin
testImplementation(libs.ktor.server.test.host)   // version.ref = "ktor" -> 3.1.1
testImplementation(libs.kotlin.testJunit)
testImplementation(libs.h2)                       // 2.2.224
```

`ContentNegotiation` on the test client comes from the server
`ktor-server-content-negotiation` dependency the app already has — install it
inside `application { }` (see below), no separate client artifact needed.

## In-memory test database (H2)

The repo pattern (see `server/src/test/kotlin/org/scent/project/ListingTestFixtures.kt`)
is a plain `Database.connect` to a uniquely-named in-memory H2 per test class,
created fresh in `@BeforeTest`. Note the Exposed 1.x `org.jetbrains.exposed.v1.*`
imports.

```kotlin
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal fun initTestDatabase() {
    Database.connect(
        // nanoTime keeps parallel test classes from sharing a DB;
        // DB_CLOSE_DELAY=-1 keeps it alive for the whole test
        "jdbc:h2:mem:some_test_${System.nanoTime()};DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )
    transaction {
        SchemaUtils.create(UsersTable, FragrancesTable, ListingsTable /* ...tables under test... */)
    }
}
```

The repo's H2 URLs deliberately omit `MODE=PostgreSQL` — the schema DSL Exposed
emits works on plain H2. Add `;MODE=PostgreSQL` only if a specific test needs
Postgres SQL syntax. H2's PostgreSQL mode is still not identical (some
`ON CONFLICT` forms, `jsonb`); for those, use Testcontainers with a real
`postgres:16` image instead.

## testApplication setup

The repo pattern (see `ListingLifecycleRoutesTest`, `MediaRoutesTest`) installs
plugins and the route group under test directly inside `application { }`, and
uses the built-in `client` — no `module()`, no `MapApplicationConfig`:

```kotlin
testApplication {
    application {
        install(ContentNegotiation) { json() }
        configureSecurity()           // plugins.configureSecurity — defaults to secret "secret"
        routing { listingRoutes() }   // just the group under test
    }

    val owner = seedUser("alice")
    val token = generateTestToken(owner)   // HMAC256("secret"), issuer/audience match configureSecurity's defaults

    val res = client.get("/api/v1/listings/$id") { bearerAuth(token) }
    assertEquals(HttpStatusCode.OK, res.status)
}
```

Only reach for `environment { config = MapApplicationConfig("jwt.secret" to ...) }`
if a test needs a *non-default* secret/issuer (e.g. asserting a mis-signed token
is rejected) — `configureSecurity()` reads `jwt.*` config when present and only
falls back to `"secret"` when it's absent.

## Minting test tokens

Match `ListingTestFixtures.generateTestToken` — same lib the server verifies with:

```kotlin
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

internal fun generateTestToken(userId: Int, secret: String = "secret"): String =
    JWT.create()
        .withAudience("fragrances-users")
        .withIssuer("fragrances-app")
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000))
        .sign(Algorithm.HMAC256(secret))
```

## Auth route tests

```kotlin
class AuthRoutesTest {
    @BeforeTest fun setUp() = initTestDatabase()

    @Test
    fun `register then me returns the registered user`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            configureSecurity()
            routing { authRoutes() }
        }

        val reg = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("alice", "alice@test.com", "password123"))
        }
        assertEquals(HttpStatusCode.OK, reg.status)
        val token = reg.body<AuthResponse>().token

        val me = client.get("/api/v1/auth/me") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, me.status)
        assertEquals("alice@test.com", me.body<UserResponse>().email)
    }

    @Test
    fun `me without token returns 401`() = testApplication {
        application { configureSecurity(); routing { authRoutes() } }
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/auth/me").status)
    }
}
```

Test naming and structure follow ADS-STE100 (`docs/architecture-guidelines.md`):
backtick names describing behavior, Arrange-Act-Assert, one behavior per test,
success + error + edge cases. Route test files are kept under detekt's
`LargeClass` threshold — split shared seed/JWT helpers into a `*TestFixtures.kt`
(see `ListingTestFixtures.kt`) rather than growing one file.

## Testing expired / mis-signed tokens

Mint tokens directly in tests with the same lib the server uses:

```kotlin
fun expiredToken(secret: String = "test-secret") = JWT.create()
    .withIssuer("fragrances-app")
    .withAudience("fragrances-users")
    .withClaim("userId", 1)
    .withExpiresAt(Date(System.currentTimeMillis() - 60_000))
    .sign(Algorithm.HMAC256(secret))

fun wrongSecretToken() = expiredToken(secret = "some-other-secret")
```

Assert both → 401. This covers the signer/verifier-mismatch failure mode at
the unit level. (The Ktor `jwt-auth-tests` sample shows the same idea for
RSA-signed tokens: build a verifier around a known test key.)

## Run

```bash
./gradlew :server:test              # all
./gradlew :server:test --tests '*AuthRoutesTest*'
```
