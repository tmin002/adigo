package kr.gachon.adigo.data.remote.core

import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.remote.auth.AuthRemoteDataSource
import kr.gachon.adigo.data.model.dto.RefreshTokenRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val authRemote: AuthRemoteDataSource
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val accessToken = tokenManager.getJwtToken()
        if (!accessToken.isNullOrBlank()) {
            request = request.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        }

        val response = chain.proceed(request)
        if (response.code == 401) {
            response.close()
            val refreshToken = tokenManager.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                // 동기적으로 새 토큰 요청
                val refreshResponse = runCatching {
                    // retrofit2는 suspend fun을 동기적으로 호출하려면 runBlocking 필요
                    kotlinx.coroutines.runBlocking {
                        authRemote.refresh(
                            RefreshTokenRequest(accessToken ?: "", refreshToken)
                        ).getOrNull()
                    }
                }.getOrNull()
                refreshResponse?.data?.let {
                    tokenManager.saveTokens(it)
                    // 새 토큰으로 원래 요청 재시도
                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer ${it.accessToken}")
                        .build()
                    return chain.proceed(newRequest)
                }
            }
        }
        return response
    }
}

object RetrofitProvider {
    private const val BASE_URL = "https://adigo.site/api/"
    // Keep the OkHttpClient instance private within the object
    private var okHttpClient: OkHttpClient? = null

    fun getOkHttpClient(tokenManager: TokenManager, authRemote: AuthRemoteDataSource): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: buildOkHttpClient(tokenManager, authRemote).also { okHttpClient = it }
        }
    }

    private fun buildOkHttpClient(tokenManager: TokenManager, authRemote: AuthRemoteDataSource): OkHttpClient {
        return OkHttpClient.Builder()
            // Add read timeout for potentially long-running requests (optional)
            .readTimeout(30, TimeUnit.SECONDS)
            // Add write timeout (optional)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Add connect timeout
            .connectTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenManager, authRemote))
             // Add other interceptors if needed (e.g., logging)
            .build()
    }

    fun create(tokenManager: TokenManager, authRemote: AuthRemoteDataSource): Retrofit {
        val client = getOkHttpClient(tokenManager, authRemote) // Use the shared OkHttpClient instance

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // Use the shared client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}