package kr.gachon.adigo.data.remote.user

import kr.gachon.adigo.data.model.dto.ProfileResponse
import okhttp3.MultipartBody
import retrofit2.Response

class UserRemoteDataSource(
    private val api: UserApi
) {
    suspend fun uploadProfileImage(file: MultipartBody.Part): Response<ProfileResponse> =
        api.uploadProfileImage(file)
} 