package kr.gachon.adigo.data.remote.user

import kr.gachon.adigo.data.model.dto.ProfileResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT
import retrofit2.http.Body

interface UserApi {
    @GET("member/me")
    suspend fun getCurrentUser(): Response<ProfileResponse>

    @Multipart
    @POST("member/profile-image")
    suspend fun uploadProfileImage(
        @Part file: MultipartBody.Part
    ): Response<ProfileResponse>

    @PUT("member/nickname")
    suspend fun updateNickname(
        @Body request: NicknameUpdateRequest
    ): Response<ProfileResponse>
}

data class NicknameUpdateRequest(
    val nickname: String
) 