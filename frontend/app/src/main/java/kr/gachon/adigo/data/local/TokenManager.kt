package kr.gachon.adigo.data.local

import android.content.Context
import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kr.gachon.adigo.data.model.dto.LoginResponse
import javax.crypto.AEADBadTagException

class TokenManager(context: Context) {

    /* ───────── 1) 암호화 prefs : JWT / Refresh ───────── */
    private val securePrefs = try {
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
        context.deleteSharedPreferences("jwt_prefs")
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

    /* ───────── 2) 일반 prefs : FCM 디바이스 토큰 ───────── */
    private val plainPrefs =
        context.getSharedPreferences("plain_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val JWT_KEY          = "jwt_token"
        private const val REFRESH_TOKEN_KEY= "refresh_token"
        private const val DEVICE_TOKEN_KEY = "device_token"
        private const val USER_EMAIL_KEY   = "user_email"
    }

    /* ───────── JWT / Refresh 저장 ───────── */
    fun saveTokens(res: LoginResponse.Response) {
        securePrefs.edit().apply {
            putString(JWT_KEY,          res.accessToken)
            putString(REFRESH_TOKEN_KEY,res.refreshToken)
        }.apply()
    }

    /* ───────── 이메일 저장 ───────── */
    fun saveUserEmail(email: String) {
        securePrefs.edit().putString(USER_EMAIL_KEY, email).apply()
    }

    /* ───────── 이메일 조회 ───────── */
    fun getUserEmail(): String? = securePrefs.getString(USER_EMAIL_KEY, null)

    /* ───────── 디바이스 토큰 저장 (일반 prefs) ───────── */
    fun saveDeviceToken(token: String) {
        val ok = plainPrefs.edit()
            .putString(DEVICE_TOKEN_KEY, token)
            .commit()             // 동기 저장
        Log.d("TokenManager", "saveDeviceToken() result=$ok value=$token")
    }

    /* ───────── 디바이스 토큰 조회 ───────── */
    fun getDeviceToken(): String? =
        plainPrefs.getString(DEVICE_TOKEN_KEY, null).also {
            Log.d("TokenManager", "getDeviceToken() = $it")
        }

    /* ───────── JWT 만료 여부 ───────── */
    fun isTokenExpired(): Boolean {
        val token = getJwtToken() ?: return true
        return try {
            val exp = JWT.decode(token).expiresAt
            exp == null || exp.before(java.util.Date()).also {
                Log.d("TokenManager", "Token expired? $it")
            }
        } catch (e: JWTDecodeException) {
            true
        }
    }

    /* ───────── 기타 getters ───────── */
    fun getJwtToken(): String?     = securePrefs.getString(JWT_KEY, null)
    fun getRefreshToken(): String? = securePrefs.getString(REFRESH_TOKEN_KEY, null)

    /* ───────── JWT·Refresh 제거 (디바이스 토큰은 유지) ───────── */
    fun clearTokens() {
        securePrefs.edit().clear().apply()
    }

    /* ───────── 현재 로그인한 사용자의 ID 조회 ───────── */
    fun getCurrentUserId(): Long? {
        val token = getJwtToken() ?: return null
        return try {
            val jwt = JWT.decode(token)
            jwt.getClaim("id").asLong()
        } catch (e: JWTDecodeException) {
            Log.e("TokenManager", "Failed to decode JWT token", e)
            null
        }
    }
}