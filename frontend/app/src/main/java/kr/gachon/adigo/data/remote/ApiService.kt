package kr.gachon.adigo.data.remote

import kr.gachon.adigo.data.model.LoginRequest
import kr.gachon.adigo.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.ResponseBody




interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}