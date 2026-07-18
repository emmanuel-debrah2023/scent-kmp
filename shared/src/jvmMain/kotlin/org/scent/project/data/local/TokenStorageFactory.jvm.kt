package org.scent.project.data.local

import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import java.util.concurrent.ConcurrentHashMap

actual class TokenStorageFactory {
    actual fun create(): TokenStorage =
        object : TokenStorage {
            private val store = ConcurrentHashMap<String, String>()

            override suspend fun saveToken(token: String): Result<Unit> =
                try {
                    store["auth_token"] = token
                    Unit.asRight()
                } catch (e: Exception) {
                    AppError.StorageError.WriteFailed(cause = e).asLeft()
                }

            override suspend fun getToken(): Result<String?> =
                try {
                    store["auth_token"].asRight()
                } catch (e: Exception) {
                    AppError.StorageError.ReadFailed(cause = e).asLeft()
                }

            override suspend fun clearToken(): Result<Unit> =
                try {
                    store.remove("auth_token")
                    Unit.asRight()
                } catch (e: Exception) {
                    AppError.StorageError.WriteFailed(cause = e).asLeft()
                }
        }
}
