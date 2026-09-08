package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.scent.project.data.remote.dto.UserResponse

interface UserApi {
    // TODO(feature/get-profile-by-id-endpoint): no GET /api/v1/users/{id} route exists yet;
    // unused until UserRepositoryImpl.refreshProfile is wired up to call it.
    suspend fun getProfile(
        userId: Int,
        token: String?,
    ): UserResponse
}

class UserApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : UserApi {
    // TODO(feature/get-profile-by-id-endpoint): no GET /api/v1/users/{id} route exists yet;
    // unused until UserRepositoryImpl.refreshProfile is wired up to call it.
    override suspend fun getProfile(
        userId: Int,
        token: String?,
    ): UserResponse =
        httpClient
            .get("$baseUrl/api/v1/users/$userId") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body()
}
