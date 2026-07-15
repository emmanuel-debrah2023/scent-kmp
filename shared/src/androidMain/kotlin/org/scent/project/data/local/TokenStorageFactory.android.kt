package org.scent.project.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

private val Context.scentAuthDataStore by preferencesDataStore(name = "scent_auth_prefs")

actual class TokenStorageFactory(
    val context: Context,
) {
    actual fun create(): TokenStorage =
        object : TokenStorage {
            private val tokenKey = stringPreferencesKey("auth_token")

            override suspend fun saveToken(token: String): Result<Unit> =
                try {
                    context.scentAuthDataStore.edit { preferences ->
                        preferences[tokenKey] = token
                    }
                    Unit.asRight()
                } catch (e: Exception) {
                    AppError.StorageError.WriteFailed(cause = e).asLeft()
                }

            override suspend fun getToken(): Result<String?> =
                try {
                    val token =
                        context.scentAuthDataStore.data
                            .map { preferences -> preferences[tokenKey] }
                            .first()
                    token.asRight()
                } catch (e: Exception) {
                    AppError.StorageError.ReadFailed(cause = e).asLeft()
                }

            override suspend fun clearToken(): Result<Unit> =
                try {
                    context.scentAuthDataStore.edit { preferences ->
                        preferences.remove(tokenKey)
                    }
                    Unit.asRight()
                } catch (e: Exception) {
                    AppError.StorageError.WriteFailed(cause = e).asLeft()
                }
        }
}
