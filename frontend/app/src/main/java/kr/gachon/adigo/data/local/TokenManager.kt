package kr.gachon.adigo.data.local

import android.content.Context
import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kr.gachon.adigo.data.model.LoginResponse
import javax.crypto.AEADBadTagException

class TokenManager(context: Context) {
    private val prefs = try {
        EncryptedSharedPreferences.create(
            context,
            "jwt_prefs",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: AEADBadTagException) {
        context.deleteSharedPreferences("jwt_prefs") // 손상된 prefs 삭제
        EncryptedSharedPreferences.create(
            context,
            "jwt_prefs",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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

    fun isTokenExpired(): Boolean {
        val token = getJwtToken() ?: return true
        return try {
            val decodedJWT = JWT.decode(token)
            val expirationDate = decodedJWT.expiresAt
            val isExpired = expirationDate.before(java.util.Date())

            if(isExpired) {
                Log.d("TokenManager", "Token is expired")
            } else {
                Log.d("TokenManager", "Token is not expired")
            }
            isExpired
        } catch (e: JWTDecodeException) {
            true
        }
    }

    fun getJwtToken(): String? = prefs.getString(JWT_KEY, null)

    fun getRefreshToken(): String? = prefs.getString(REFRESH_TOKEN_KEY, null)

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
