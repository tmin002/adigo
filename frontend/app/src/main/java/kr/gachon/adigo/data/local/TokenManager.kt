package kr.gachon.adigo.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kr.gachon.adigo.data.model.LoginResponse

class TokenManager(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "jwt_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val JWT_KEY = "jwt_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    fun saveTokens(response: LoginResponse.Response) {
        prefs.edit().apply {
            putString(JWT_KEY, response.accessToken)
            putString(REFRESH_TOKEN_KEY, response.refreshToken)
            apply()
        }
    }

    fun getJwtToken(): String? = prefs.getString(JWT_KEY, null)

    fun getRefreshToken(): String? = prefs.getString(REFRESH_TOKEN_KEY, null)

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
