package providers

import config.ImageConfig
import config.StreamConfig
import org.koin.dsl.module

fun providerModule(
    stream: StreamConfig,
    image: ImageConfig,
) = module {
    single<StreamProvider> {
        when (stream) {
            StreamConfig.Fake -> FakeStreamProvider()
            is StreamConfig.Cloudflare ->
                CloudflareStreamProvider(
                    accountId = stream.accountId,
                    apiToken = stream.apiToken,
                    webhookSecret = stream.webhookSecret,
                )
        }
    }
    single<ImageProvider> {
        when (image) {
            ImageConfig.Fake -> FakeImageProvider()
            is ImageConfig.Supabase ->
                SupabaseStorageProvider(
                    projectUrl = image.url,
                    serviceRoleKey = image.serviceRoleKey,
                    bucket = image.bucket,
                )
        }
    }
    // Bound only under fake image config, so its presence is what mounts the fake image routes.
    if (image is ImageConfig.Fake) single { FakeImageStore() }
}
