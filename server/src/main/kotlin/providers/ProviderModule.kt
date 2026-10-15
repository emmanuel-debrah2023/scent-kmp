package providers

import org.koin.dsl.module

fun providerModule(
    fakeStream: Boolean,
    fakeImage: Boolean,
) = module {
    single<StreamProvider> {
        if (fakeStream) {
            FakeStreamProvider()
        } else {
            CloudflareStreamProvider(
                accountId = System.getProperty("CLOUDFLARE_ACCOUNT_ID") ?: "",
                apiToken = System.getProperty("CLOUDFLARE_API_TOKEN") ?: "",
                webhookSecret = System.getProperty("CLOUDFLARE_WEBHOOK_SECRET") ?: "",
            )
        }
    }
    single<ImageProvider> {
        if (fakeImage) {
            FakeImageProvider()
        } else {
            SupabaseStorageProvider(
                projectUrl = System.getProperty("SUPABASE_URL") ?: "",
                serviceRoleKey = System.getProperty("SUPABASE_SERVICE_ROLE_KEY") ?: "",
                bucket = System.getProperty("SUPABASE_STORAGE_BUCKET") ?: "listing-photos",
            )
        }
    }
    // Bound only under fake image config, so its presence is what mounts the fake image routes.
    if (fakeImage) single { FakeImageStore() }
}
