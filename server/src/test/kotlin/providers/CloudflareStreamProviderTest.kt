package providers

import kotlin.test.Test
import kotlin.test.assertFalse

class CloudflareStreamProviderTest {
    private fun provider(webhookSecret: String) =
        CloudflareStreamProvider(
            accountId = "unused-in-tests",
            apiToken = "unused-in-tests",
            webhookSecret = webhookSecret,
        )

    private fun freshHeader() = "time=${System.currentTimeMillis() / 1000},sig1=00"

    @Test
    fun `verifyWebhookSignature returns false when the secret is empty`() {
        assertFalse(provider(webhookSecret = "").verifyWebhookSignature("{}", freshHeader()))
    }

    @Test
    fun `verifyWebhookSignature returns false when the secret is blank`() {
        assertFalse(provider(webhookSecret = "   ").verifyWebhookSignature("{}", freshHeader()))
    }
}
