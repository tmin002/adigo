package kr.gachon.adigo.data.remote.push

import kr.gachon.adigo.data.model.dto.newPushTokenDto
import kr.gachon.adigo.data.model.dto.newPushTokenResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// push/PushApi.kt
interface PushApi {
    @POST("push/token/register")
    suspend fun register(@Body body: newPushTokenDto): Response<newPushTokenResponseDto>
}