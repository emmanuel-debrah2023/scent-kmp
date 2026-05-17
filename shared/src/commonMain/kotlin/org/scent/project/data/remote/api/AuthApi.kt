package org.scent.project.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.scent.project.data.remote.dto.AuthResponseDto

class AuthApi(private val client: HttpClient) {
    private val baseUrl = "http://10.0.2.2:8080" // Local dev for Android

    suspend fun register(request: Map<String, String>): AuthResponseDto {
        return client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun login(request: Map<String, String>): AuthResponseDto {
        return client.post("$baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun googleAuth(idToken: String): AuthResponseDto {
        return client.post("$baseUrl/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("idToken" to idToken))
        }.body()
    }

    suspend fun appleAuth(identityToken: String, email: String?, givenName: String?): AuthResponseDto {
        return client.post("$baseUrl/api/v1/auth/apple") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "identityToken" to identityToken,
                "email" to email,
                "givenName" to givenName
            ))
        }.body()
    }

    suspend fun getMe(token: String): AuthResponseDto {
        return client.get("$baseUrl/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }
}
