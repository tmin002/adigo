package kr.gachon.adigo.data.remote.core

import kr.gachon.adigo.data.local.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {
    private const val BASE_URL = "https://adigo.site/api/"

    fun create(tokenManager: TokenManager): Retrofit {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                tokenManager.getJwtToken()
                    ?.takeIf { !tokenManager.isTokenExpired() }
                    ?.let { jwt -> requestBuilder.addHeader("Authorization", "Bearer $jwt") }
                chain.proceed(requestBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}