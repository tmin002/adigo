package kr.gachon.adigo.data.remote.friend

import kr.gachon.adigo.data.model.dto.FriendDeleteResponse
import kr.gachon.adigo.data.model.dto.FriendListResponse
import kr.gachon.adigo.data.model.dto.ProfileResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// friend/FriendApi.kt
interface FriendApi {
    @GET("member/friend/list")
    suspend fun friendList(): Response<FriendListResponse>

    @DELETE("member/friend/{friendEmail}")
    suspend fun deleteFriend(@Path("friendEmail") friendEmail: String): Response<FriendDeleteResponse>

    @POST("member/friend/{friendEmail}")
    suspend fun addFriend(@Path("friendEmail") friendEmail: String): Response<ProfileResponse>




}