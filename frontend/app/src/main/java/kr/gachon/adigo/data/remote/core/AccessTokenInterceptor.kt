package kr.gachon.adigo.data.remote.core

import kr.gachon.adigo.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AccessTokenInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getJwtToken()
        val req = if (token.isNullOrBlank())
            chain.request()
        else
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()

        return chain.proceed(req)
    }
}