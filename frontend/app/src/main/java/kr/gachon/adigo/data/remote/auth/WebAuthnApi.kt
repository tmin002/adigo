package kr.gachon.adigo.data.remote.auth

import kr.gachon.adigo.data.model.dto.webauthn.AssertionRequestDto
import kr.gachon.adigo.data.model.dto.webauthn.AssertionResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WebAuthnApi {
    @GET("webauthn/assertion/options")
    suspend fun getAssertionOptions(): Response<AssertionRequestDto>

    @POST("webauthn/assertion/result")
    suspend fun verifyAssertion(@Body body: AssertionResponseDto): Response<Unit>
}