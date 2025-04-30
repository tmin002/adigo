package kr.gachon.adigo.data.remote

import kr.gachon.adigo.data.model.dto.CheckDuplicateEmailResponse
import kr.gachon.adigo.data.model.dto.CheckDuplicateNumberResponse
import kr.gachon.adigo.data.model.dto.FriendListResponse
import kr.gachon.adigo.data.model.dto.LoginRequest
import kr.gachon.adigo.data.model.dto.LoginResponse
import kr.gachon.adigo.data.model.dto.RefreshTokenRequest
import kr.gachon.adigo.data.model.dto.SignUpRequest
import kr.gachon.adigo.data.model.dto.SignUpResponse
import kr.gachon.adigo.data.model.dto.newPushTokenDto
import kr.gachon.adigo.data.model.dto.newPushTokenResponseDto
import kr.gachon.adigo.data.model.dto.smsResponse
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

    @GET("auth/check/sendSMS")
    suspend fun sendSMS(@Query("to") phoneNumber: String): Response<smsResponse>

    @GET("auth/check/verifySMS")
    suspend fun verifySMS(@Query("to") phoneNumber: String, @Query("code") code: String): Response<smsResponse>

    @GET("auth/check/duplicatedEmail")
    suspend fun checkDuplicateEmail(@Query("email") email: String): Response<CheckDuplicateEmailResponse>

    @GET("auth/check/duplicatedNumber")
    suspend fun checkDuplicateNumber(@Query("phonenumber") number: String): Response<CheckDuplicateNumberResponse>

    @POST("auth/reissue")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @POST("push/token/register")
    suspend fun registerDeviceToken(@Body request: newPushTokenDto): Response<newPushTokenResponseDto>

    @GET("/member/friend/list")
    suspend fun getFriendList(): Response<FriendListResponse>

}