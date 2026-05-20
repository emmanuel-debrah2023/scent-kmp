package org.scent.project.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
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
    private val baseUrl: String
) : AuthApi {

    override suspend fun register(request: RegisterRequest): AuthResponse {
        return httpClient.post("$baseUrl/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun login(request: LoginRequest): AuthResponse {
        return httpClient.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getCurrentUser(token: String): MeResponse {
        return httpClient.get("$baseUrl/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }
}
