package kr.gachon.adigo.data.remote.core

import kr.gachon.adigo.data.local.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    private const val BASE_URL = "https://adigo.site/api/"
    // Keep the OkHttpClient instance private within the object
    private var okHttpClient: OkHttpClient? = null

    fun getOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: buildOkHttpClient(tokenManager).also { okHttpClient = it }
        }
    }

    private fun buildOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return OkHttpClient.Builder()
            // Add read timeout for potentially long-running requests (optional)
            .readTimeout(30, TimeUnit.SECONDS)
            // Add write timeout (optional)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Add connect timeout
            .connectTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()

                // Add JWT header to standard HTTP requests IF the token is valid
                tokenManager.getJwtToken()
                    ?.takeIf { !tokenManager.isTokenExpired() }
                    ?.let { jwt ->
                        requestBuilder.addHeader("Authorization", "Bearer $jwt")
                    }

                chain.proceed(requestBuilder.build())
            }
             // Add other interceptors if needed (e.g., logging)
            .build()
    }

    fun create(tokenManager: TokenManager): Retrofit {
        val client = getOkHttpClient(tokenManager) // Use the shared OkHttpClient instance

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // Use the shared client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}