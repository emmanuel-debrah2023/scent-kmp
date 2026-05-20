// TODO: migrate to Keychain before production
package org.scent.project.data.local

import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import platform.Foundation.NSUserDefaults

actual class TokenStorageFactory() {

    actual fun create(): TokenStorage = object : TokenStorage {

        private val defaults = NSUserDefaults.standardUserDefaults
        private val tokenKey = "auth_token"

        override suspend fun saveToken(token: String): Result<Unit> {
            return try {
                defaults.setObject(token, forKey = tokenKey)
                Unit.asRight()
            } catch (e: Exception) {
                AppError.StorageError.WriteFailed(cause = e).asLeft()
            }
        }

        override suspend fun getToken(): Result<String?> {
            return try {
                val token = defaults.stringForKey(tokenKey)
                token.asRight()
            } catch (e: Exception) {
                AppError.StorageError.ReadFailed(cause = e).asLeft()
            }
        }

        override suspend fun clearToken(): Result<Unit> {
            return try {
                defaults.removeObjectForKey(tokenKey)
                Unit.asRight()
            } catch (e: Exception) {
                AppError.StorageError.WriteFailed(cause = e).asLeft()
            }
        }
    }
}
