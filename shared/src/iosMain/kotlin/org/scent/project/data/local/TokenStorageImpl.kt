package org.scent.project.data.local

import platform.Foundation.*
import platform.Security.*
import kotlinx.cinterop.*
import platform.CoreFoundation.*

@OptIn(ExperimentalForeignApi::class)
class TokenStorageImpl : TokenStorage {
    private val service = "org.scent.project.auth"
    private val account = "auth_token"

    override suspend fun saveToken(token: String) {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service.toCFType())
        CFDictionaryAddValue(query, kSecAttrAccount, account.toCFType())
        
        val data = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        CFDictionaryAddValue(query, kSecValueData, data?.toCFType())

        SecItemDelete(query)
        SecItemAdd(query, null)
    }

    override suspend fun getToken(): String? {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service.toCFType())
        CFDictionaryAddValue(query, kSecAttrAccount, account.toCFType())
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val result = memScoped {
            val ptr = alloc<ObjCObjectVar<NSData?>>()
            val status = SecItemCopyMatching(query, ptr.ptr.reinterpret())
            if (status == errSecSuccess) ptr.value else null
        }

        return result?.let {
            NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
        }
    }

    override suspend fun clearToken() {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service.toCFType())
        CFDictionaryAddValue(query, kSecAttrAccount, account.toCFType())
        SecItemDelete(query)
    }
    
    private fun Any?.toCFType() = this as CFTypeRef?
}
