package org.scent.project.data.remote

import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.Test
import kotlin.test.assertEquals

private const val THIRTY_SECONDS_MILLIS = 30_000

class HttpClientFactoryTest {
    // OkHttp's own default is 10 s, which fired before the 30 s request timeout and showed a slow
    // server as "You are offline". The interceptor reads the timeout without opening a socket.
    @Test
    fun `the Android client waits as long for a slow reply as the request timeout allows`() =
        runTest {
            var readTimeout = 0
            var writeTimeout = 0
            val client =
                createOkHttpClient {
                    addInterceptor(
                        Interceptor { chain ->
                            readTimeout = chain.readTimeoutMillis()
                            writeTimeout = chain.writeTimeoutMillis()
                            Response
                                .Builder()
                                .request(chain.request())
                                .protocol(Protocol.HTTP_1_1)
                                .code(200)
                                .message("OK")
                                .body("".toResponseBody())
                                .build()
                        },
                    )
                }

            client.get("https://example.test")

            assertEquals(THIRTY_SECONDS_MILLIS, readTimeout)
            assertEquals(THIRTY_SECONDS_MILLIS, writeTimeout)
        }
}
