package kr.gachon.adigo.data.remote.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.remote.auth.AuthApiSync        // 동기용 인터페이스
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    private const val BASE_URL = "https://adigo.site/api/"
    @Volatile private var retrofit: Retrofit? = null       // double-checked locking

    /** 앱 전역에서 하나만 얻어오기 */
    fun provide(tokenManager: TokenManager): Retrofit =
        retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(tokenManager).also { retrofit = it }
        }

    // ───────────────────────────────────────────────────────────────
    // internal
    private fun buildRetrofit(tokenManager: TokenManager): Retrofit {
        /** 0) 공통 OkHttpBuilder */
        val baseBuilder = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)

        /** ⭐️ 커스텀 Gson 설정 */
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
                LocalDateTime.parse(json.asString)
            })
            .create()

        /** 1) 동기 호출용 AuthApiSync (임시 Retrofit, 인터셉터 없음) */
        val authApiSync = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(baseBuilder.build())                   // 순수 client
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AuthApiSync::class.java)

        /** 2) 최종 OkHttpClient : 헤더 Interceptor + TokenAuthenticator */
        val finalClient = baseBuilder
            .addInterceptor(AccessTokenInterceptor(tokenManager))
            .authenticator(TokenAuthenticator(tokenManager, authApiSync))
            .build()

        /** 3) 앱에서 실제로 쓰는 Retrofit 인스턴스 */
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(finalClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}