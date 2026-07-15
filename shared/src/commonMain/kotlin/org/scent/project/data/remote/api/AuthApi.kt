package org.scent.project.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.scent.project.data.remote.dto.AuthResponse
import org.scent.project.data.remote.dto.LoginRequest
import org.scent.project.data.remote.dto.MeResponse
import org.scent.project.data.remote.dto.RegisterRequest

interface AuthApi {
    suspend fun register(request: RegisterRequest): AuthResponse

    suspend fun login(request: LoginRequest): AuthResponse

    suspend fun getCurrentUser(token: String): MeResponse
}

class AuthApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : AuthApi {
    override suspend fun register(request: RegisterRequest): AuthResponse =
        httpClient
            .post("$baseUrl/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

    override suspend fun login(request: LoginRequest): AuthResponse =
        httpClient
            .post("$baseUrl/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

    override suspend fun getCurrentUser(token: String): MeResponse =
        httpClient
            .get("$baseUrl/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
}
