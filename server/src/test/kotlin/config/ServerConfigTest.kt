package config

import io.ktor.server.config.MapApplicationConfig
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ServerConfigTest {
    private val prodEntries =
        mapOf(
            "jwt.secret" to "jwt-secret",
            "database.url" to "jdbc:postgresql://db/scent",
            "cloudflare.accountId" to "cf-account",
            "cloudflare.apiToken" to "cf-token",
            "cloudflare.webhookSecret" to "cf-webhook",
            "supabase.url" to "https://project.supabase.co",
            "supabase.serviceRoleKey" to "service-role-key",
        )

    private fun load(
        entries: Map<String, String> = prodEntries,
        vararg overrides: Pair<String, String>,
    ) = loadServerConfig(MapApplicationConfig(*(entries + overrides).toList().toTypedArray()))

    private fun assertInvalidInput(
        fieldName: String,
        result: Result<ServerConfig>,
    ) {
        val error = assertIs<AppError.ValidationError.InvalidInput>(result.leftOrNull())
        assertEquals(fieldName, error.fieldName)
    }

    private fun without(vararg keys: String) = prodEntries - keys.toSet()

    @Test
    fun `a full production config loads with the exact values`() {
        val config = load().getOrNull()

        assertEquals(RuntimeMode.PROD, config?.mode)
        assertEquals("jwt-secret", config?.jwt?.secret)
        assertEquals("jdbc:postgresql://db/scent", config?.database?.url)
        assertEquals(StreamConfig.Cloudflare("cf-account", "cf-token", "cf-webhook"), config?.stream)
        assertEquals(
            ImageConfig.Supabase("https://project.supabase.co", "service-role-key", "listing-photos"),
            config?.image,
        )
        assertFalse(config?.devRoutes ?: true)
    }

    @Test
    fun `a missing JWT_SECRET is rejected naming the key`() {
        assertEquals(AppError.ValidationError.RequiredFieldEmpty("JWT_SECRET").asLeft(), load(without("jwt.secret")))
    }

    @Test
    fun `a missing DATABASE_URL is rejected naming the key`() {
        assertEquals(
            AppError.ValidationError.RequiredFieldEmpty("DATABASE_URL").asLeft(),
            load(without("database.url")),
        )
    }

    @Test
    fun `a missing CLOUDFLARE_WEBHOOK_SECRET is rejected naming the key`() {
        val result = load(without("cloudflare.webhookSecret"))

        assertEquals(AppError.ValidationError.RequiredFieldEmpty("CLOUDFLARE_WEBHOOK_SECRET").asLeft(), result)
    }

    @Test
    fun `a missing SUPABASE_URL is rejected naming the key`() {
        assertEquals(
            AppError.ValidationError.RequiredFieldEmpty("SUPABASE_URL").asLeft(),
            load(without("supabase.url")),
        )
    }

    @Test
    fun `a missing SUPABASE_SERVICE_ROLE_KEY is rejected naming the key`() {
        val result = load(without("supabase.serviceRoleKey"))

        assertEquals(AppError.ValidationError.RequiredFieldEmpty("SUPABASE_SERVICE_ROLE_KEY").asLeft(), result)
    }

    @Test
    fun `a blank secret counts as missing`() {
        val result = load(prodEntries, "jwt.secret" to "   ")

        assertEquals(AppError.ValidationError.RequiredFieldEmpty("JWT_SECRET").asLeft(), result)
    }

    @Test
    fun `every missing key is named in one error`() {
        val result = load(without("jwt.secret", "supabase.url"))

        assertEquals(AppError.ValidationError.RequiredFieldEmpty("JWT_SECRET, SUPABASE_URL").asLeft(), result)
    }

    @Test
    fun `dev mode with fake providers needs no Cloudflare or Supabase keys`() {
        val entries =
            mapOf(
                "scent.mode" to "dev",
                "scent.streamProvider" to "fake",
                "scent.imageProvider" to "fake",
                "jwt.secret" to "jwt-secret",
                "database.url" to "jdbc:postgresql://db/scent",
            )

        val config = load(entries).getOrNull()

        assertEquals(RuntimeMode.DEV, config?.mode)
        assertEquals(StreamConfig.Fake, config?.stream)
        assertEquals(ImageConfig.Fake, config?.image)
    }

    @Test
    fun `dev mode still requires JWT_SECRET and DATABASE_URL`() {
        val result =
            load(mapOf("scent.mode" to "dev", "scent.streamProvider" to "fake", "scent.imageProvider" to "fake"))

        assertEquals(AppError.ValidationError.RequiredFieldEmpty("JWT_SECRET, DATABASE_URL").asLeft(), result)
    }

    @Test
    fun `an unset SCENT_ENV means prod`() {
        assertEquals(RuntimeMode.PROD, load().getOrNull()?.mode)
    }

    @Test
    fun `an unknown SCENT_ENV is rejected`() {
        val result = load(prodEntries, "scent.mode" to "staging")

        assertInvalidInput("SCENT_ENV", result)
    }

    @Test
    fun `prod mode refuses a fake stream provider`() {
        val result = load(prodEntries, "scent.streamProvider" to "fake")

        assertInvalidInput("STREAM_PROVIDER", result)
    }

    @Test
    fun `prod mode refuses a fake image provider`() {
        val result = load(prodEntries, "scent.imageProvider" to "fake")

        assertInvalidInput("IMAGE_PROVIDER", result)
    }

    @Test
    fun `prod mode refuses DEV_ROUTES`() {
        val result = load(prodEntries, "scent.devRoutes" to "true")

        assertInvalidInput("DEV_ROUTES", result)
    }

    @Test
    fun `dev routes are on only when DEV_ROUTES is exactly true`() {
        val dev = prodEntries + ("scent.mode" to "dev")

        assertTrue(load(dev, "scent.devRoutes" to "true").getOrNull()?.devRoutes == true)
        assertFalse(load(dev, "scent.devRoutes" to "yes").getOrNull()?.devRoutes ?: true)
        assertFalse(load(dev).getOrNull()?.devRoutes ?: true)
    }

    @Test
    fun `toString never prints a secret`() {
        val rendered = load().getOrNull().toString()

        listOf("jwt-secret", "cf-token", "cf-webhook", "service-role-key").forEach {
            assertFalse(rendered.contains(it), "toString leaked $it")
        }
        assertIs<ServerConfig>(load().getOrNull())
    }
}
