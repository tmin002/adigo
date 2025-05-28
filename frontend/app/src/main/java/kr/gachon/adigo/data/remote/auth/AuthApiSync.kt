package kr.gachon.adigo.data.remote.auth

import kr.gachon.adigo.data.model.dto.LoginResponse
import kr.gachon.adigo.data.model.dto.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiSync {
    @POST("auth/reissue")
    fun refreshSync(@Body body: RefreshTokenRequest): retrofit2.Call<LoginResponse>
}