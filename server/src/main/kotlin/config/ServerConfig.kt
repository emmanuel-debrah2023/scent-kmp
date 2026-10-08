package config

import io.ktor.server.config.ApplicationConfig
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

private const val DEFAULT_BUCKET = "listing-photos"
private const val FAKE_PROVIDER = "fake"

/** `SCENT_ENV`. Unset means [PROD], so a missing flag fails closed. */
enum class RuntimeMode { PROD, DEV }

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
) {
    override fun toString() = "JwtConfig(issuer=$issuer, audience=$audience, realm=$realm, secret=***)"
}

data class DatabaseConfig(
    val url: String,
    val user: String = "",
    val password: String = "",
) {
    override fun toString() = "DatabaseConfig(url=$url, user=$user, password=***)"
}

sealed interface StreamConfig {
    data object Fake : StreamConfig

    data class Cloudflare(
        val accountId: String,
        val apiToken: String,
        val webhookSecret: String,
    ) : StreamConfig {
        override fun toString() = "Cloudflare(accountId=$accountId, apiToken=***, webhookSecret=***)"
    }
}

sealed interface ImageConfig {
    data object Fake : ImageConfig

    data class Supabase(
        val url: String,
        val serviceRoleKey: String,
        val bucket: String = DEFAULT_BUCKET,
    ) : ImageConfig {
        override fun toString() = "Supabase(url=$url, bucket=$bucket, serviceRoleKey=***)"
    }
}

data class ServerConfig(
    val mode: RuntimeMode,
    val jwt: JwtConfig,
    val database: DatabaseConfig,
    val stream: StreamConfig,
    val image: ImageConfig,
    val devRoutes: Boolean,
)

/**
 * Reads and validates the whole server configuration in one place so a missing
 * secret stops the server at boot rather than failing open at request time.
 *
 * Requirements follow the provider choice: a real stream provider needs the Cloudflare
 * keys, a real image provider needs the Supabase keys. Fake providers and dev routes are
 * only allowed when `SCENT_ENV=dev`. Every missing key is reported in a single error.
 */
fun loadServerConfig(config: ApplicationConfig): Result<ServerConfig> {
    val value = { path: String ->
        config
            .propertyOrNull(path)
            ?.getString()
            ?.trim()
            .orEmpty()
    }

    val mode =
        when (val raw = value("scent.mode")) {
            "", "prod" -> RuntimeMode.PROD
            "dev" -> RuntimeMode.DEV
            else -> return invalidInput("SCENT_ENV", "Invalid SCENT_ENV '$raw', expected prod or dev")
        }
    val fakeStream = value("scent.streamProvider") == FAKE_PROVIDER
    val fakeImage = value("scent.imageProvider") == FAKE_PROVIDER
    val devRoutes = value("scent.devRoutes") == "true"

    val devOnly = devOnlyFlagsSet(mode, fakeStream, fakeImage, devRoutes)
    if (devOnly.isNotEmpty()) {
        val names = devOnly.joinToString()
        return invalidInput(names, "$names may only be set when SCENT_ENV=dev")
    }

    val missing = missingKeys(value, fakeStream, fakeImage)
    if (missing.isNotEmpty()) {
        return AppError.ValidationError.RequiredFieldEmpty(missing.joinToString()).asLeft()
    }

    return ServerConfig(
        mode = mode,
        jwt = readJwt(value),
        database = DatabaseConfig(value("database.url"), value("database.user"), value("database.password")),
        stream = if (fakeStream) StreamConfig.Fake else readCloudflare(value),
        image = if (fakeImage) ImageConfig.Fake else readSupabase(value),
        devRoutes = devRoutes,
    ).asRight()
}

private fun invalidInput(
    fieldName: String,
    message: String,
): Result<Nothing> = AppError.ValidationError.InvalidInput(fieldName, message).asLeft()

private fun devOnlyFlagsSet(
    mode: RuntimeMode,
    fakeStream: Boolean,
    fakeImage: Boolean,
    devRoutes: Boolean,
): List<String> =
    if (mode == RuntimeMode.DEV) {
        emptyList()
    } else {
        listOf("STREAM_PROVIDER" to fakeStream, "IMAGE_PROVIDER" to fakeImage, "DEV_ROUTES" to devRoutes)
            .filter { (_, isSet) -> isSet }
            .map { (name, _) -> name }
    }

private fun missingKeys(
    value: (String) -> String,
    fakeStream: Boolean,
    fakeImage: Boolean,
): List<String> {
    val required =
        buildList {
            add("JWT_SECRET" to "jwt.secret")
            add("DATABASE_URL" to "database.url")
            if (!fakeStream) {
                add("CLOUDFLARE_ACCOUNT_ID" to "cloudflare.accountId")
                add("CLOUDFLARE_API_TOKEN" to "cloudflare.apiToken")
                add("CLOUDFLARE_WEBHOOK_SECRET" to "cloudflare.webhookSecret")
            }
            if (!fakeImage) {
                add("SUPABASE_URL" to "supabase.url")
                add("SUPABASE_SERVICE_ROLE_KEY" to "supabase.serviceRoleKey")
            }
        }
    return required.filter { (_, path) -> value(path).isEmpty() }.map { (name, _) -> name }
}

private fun readJwt(value: (String) -> String) =
    JwtConfig(
        secret = value("jwt.secret"),
        issuer = value("jwt.issuer").ifEmpty { "fragrances-app" },
        audience = value("jwt.audience").ifEmpty { "fragrances-users" },
        realm = value("jwt.realm").ifEmpty { "fragrances" },
    )

private fun readCloudflare(value: (String) -> String) =
    StreamConfig.Cloudflare(
        accountId = value("cloudflare.accountId"),
        apiToken = value("cloudflare.apiToken"),
        webhookSecret = value("cloudflare.webhookSecret"),
    )

private fun readSupabase(value: (String) -> String) =
    ImageConfig.Supabase(
        url = value("supabase.url"),
        serviceRoleKey = value("supabase.serviceRoleKey"),
        bucket = value("supabase.bucket").ifEmpty { DEFAULT_BUCKET },
    )
