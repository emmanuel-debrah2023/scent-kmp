package org.scent.project.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRef
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
class TokenStorageImpl : TokenStorage {

    private val service = "org.scent.project.auth"
    private val account = "auth_token"

    override suspend fun saveToken(token: String): Result<Unit> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, service.toCFType())
            CFDictionaryAddValue(query, kSecAttrAccount, account.toCFType())

            val data = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            CFDictionaryAddValue(query, kSecValueData, data?.toCFType())

            // Delete any existing item first, then add the new one
            SecItemDelete(query)
            val status = SecItemAdd(query, null)

            if (status == errSecSuccess) {
                Unit.asRight()
            } else {
                AppError.StorageError.WriteFailed(
                    cause = RuntimeException("Keychain SecItemAdd failed with status: $status")
                ).asLeft()
            }
        } catch (e: Exception) {
            AppError.StorageError.WriteFailed(cause = e).asLeft()
        }
    }

    override suspend fun getToken(): Result<String?> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, service.toCFType())
            CFDictionaryAddValue(query, kSecAttrAccount, account.toCFType())
            CFDictionaryAddValue(query, kSecReturnData, true.toCFType())
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

            val token = memScoped {
                val ptr = alloc<ObjCObjectVar<NSData?>>()
                val status = SecItemCopyMatching(query, ptr.ptr.reinterpret())
                when (status) {
                    errSecSuccess -> ptr.value?.let {
                        NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
                    }
                    // errSecItemNotFound (-25300) — no token stored, not an error
                    -25300 -> null
                    else -> return AppError.StorageError.ReadFailed(
                        cause = RuntimeException("Keychain SecItemCopyMatching failed with status: $status")
                    ).asLeft()
                }
            }
            token.asRight()
        } catch (e: Exception) {
            AppError.StorageError.ReadFailed(cause = e).asLeft()
        }
    }

    override suspend fun clearToken(): Result<Unit> {
        return try {
            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, service.toCFType())
            CFDictionaryAddValue(query, kSecAttrAccount, account.toCFType())

            val status = SecItemDelete(query)

            // errSecItemNotFound means there was nothing to delete — that's fine
            if (status == errSecSuccess || status == -25300) {
                Unit.asRight()
            } else {
                AppError.StorageError.WriteFailed(
                    cause = RuntimeException("Keychain SecItemDelete failed with status: $status")
                ).asLeft()
            }
        } catch (e: Exception) {
            AppError.StorageError.WriteFailed(cause = e).asLeft()
        }
    }

    private fun Any?.toCFType() = this as CFTypeRef?
}
