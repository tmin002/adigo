package kr.gachon.adigo.data.remote

import kr.gachon.adigo.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query




interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/signup")
    suspend fun signup(@Body request: SignUpRequest): Response<SignUpResponse>

    @GET("auth/check/sendSMS?")
    suspend fun sendSMS(@Query("to") phoneNumber: String): Response<SendSmsResponse>

}