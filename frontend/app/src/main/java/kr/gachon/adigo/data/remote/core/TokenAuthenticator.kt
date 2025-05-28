package kr.gachon.adigo.data.remote.core

import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.model.dto.RefreshTokenRequest
import kr.gachon.adigo.data.remote.auth.AuthApi
import kr.gachon.adigo.data.remote.auth.AuthApiSync
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val authApiSync: AuthApiSync       // Retrofit 으로 만든 실제 API
) : Authenticator {

    @Synchronized                     // 여러 스레드 동시 401 방어
    override fun authenticate(route: Route?, response: Response): Request? {

        // ① 이미 한 번 재시도한 요청이면 중단 (무한 루프 방지)
        if (responseCount(response) >= 2) return null

        // ② 토큰이 갱신된 상태인지 확인
        val current = tokenManager.getJwtToken() ?: return null
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        if (current != requestToken) {
            // 다른 스레드가 이미 갱신했음 → 새 토큰으로 재요청
            return response.request.newBuilder()
                .header("Authorization", "Bearer $current")
                .build()
        }

        // ③ 아직 갱신 안 됐으면 Refresh 시도 (blocking)
        val refreshToken = tokenManager.getRefreshToken() ?: return null
        val refreshCall = authApiSync.refreshSync(
            RefreshTokenRequest(current, refreshToken)
        )
        val body = refreshCall.execute().body()?.data ?: return null

        tokenManager.saveTokens(body)      // Access/Refresh 동시 저장

        // ④ 새 토큰으로 원래 요청 재시도
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${body.accessToken}")
            .build()
    }

    private fun responseCount(resp: Response): Int {
        var result = 1
        var prior = resp.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}