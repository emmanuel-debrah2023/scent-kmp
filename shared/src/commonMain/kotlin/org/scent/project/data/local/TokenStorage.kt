package org.scent.project.data.local

import org.scent.project.domain.util.Result

interface TokenStorage {
    suspend fun saveToken(token: String): Result<Unit>
    suspend fun getToken(): Result<String?>
    suspend fun clearToken(): Result<Unit>
}

expect class TokenStorageFactory {
    fun create(): TokenStorage
}
