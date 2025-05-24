package kr.gachon.adigo.data.remote.user

import kr.gachon.adigo.data.model.dto.ProfileResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UserApi {
    @Multipart
    @POST("member/profile/image")
    suspend fun uploadProfileImage(
        @Part file: MultipartBody.Part
    ): Response<ProfileResponse>
} 