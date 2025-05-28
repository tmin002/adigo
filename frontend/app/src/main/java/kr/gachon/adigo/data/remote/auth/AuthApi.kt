package kr.gachon.adigo.data.remote.auth

import kr.gachon.adigo.data.model.dto.CheckDuplicateEmailResponse
import kr.gachon.adigo.data.model.dto.CheckDuplicateNumberResponse
import kr.gachon.adigo.data.model.dto.LoginRequest
import kr.gachon.adigo.data.model.dto.LoginResponse
import kr.gachon.adigo.data.model.dto.RefreshTokenRequest
import kr.gachon.adigo.data.model.dto.SignUpRequest
import kr.gachon.adigo.data.model.dto.SignUpResponse
import kr.gachon.adigo.data.model.dto.smsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
// auth/AuthApi.kt
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/signup")
    suspend fun signup(@Body body: SignUpRequest): Response<SignUpResponse>

    @GET("auth/check/sendSMS")
    suspend fun sendSMS(@Query("to") phone: String): Response<smsResponse>

    @GET("auth/check/verifySMS")
    suspend fun verifySMS(
        @Query("to") phone: String,
        @Query("code") code: String
    ): Response<smsResponse>

    @GET("auth/check/duplicatedEmail")
    suspend fun duplicatedEmail(@Query("email") email: String): Response<CheckDuplicateEmailResponse>

    @GET("auth/check/duplicatedNumber")
    suspend fun duplicatedNumber(@Query("phonenumber") phone: String): Response<CheckDuplicateNumberResponse>

    @POST("auth/reissue")
    suspend fun refresh(@Body body: RefreshTokenRequest): Response<LoginResponse>

}