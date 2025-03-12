package kr.gachon.adigo.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenManager(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKey(context),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val JWT_KEY = "jwt_token"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(JWT_KEY, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(JWT_KEY, null)
    }

    fun clearToken() {
        prefs.edit().remove(JWT_KEY).apply()
    }
}
