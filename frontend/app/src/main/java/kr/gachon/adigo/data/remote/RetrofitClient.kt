package kr.gachon.adigo.data.remote

import kr.gachon.adigo.data.local.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// 3. RetrofitClient.kt
object httpClient {
    private const val BASE_URL = "https://adigo.site/api/"

    fun create(tokenManager: TokenManager): ApiService {



        //jwt토큰이 기기에 저장되어있지 않으면 일반 retrofit 인스턴스를 돌려줌
        if(tokenManager.getJwtToken()==null){
            var okHttpClient = OkHttpClient.Builder().build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)

        }else if(tokenManager.isTokenExpired()==false){  //이미 jwt토큰이 기기에 저장되어있고 토큰이 유효하면 jwt 토큰을 헤더에 포함
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                    tokenManager.getJwtToken()?.let { token ->
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    chain.proceed(requestBuilder.build())
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }


        var okHttpClient = OkHttpClient.Builder().build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)



    }
}
