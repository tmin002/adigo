package kr.gachon.adigo.data.remote.friend

import kr.gachon.adigo.data.model.dto.FriendListResponse
import retrofit2.Response
import retrofit2.http.GET

// friend/FriendApi.kt
interface FriendApi {
    @GET("member/friend/list")
    suspend fun friendList(): Response<FriendListResponse>
}