package kr.gachon.adigo.data.remote.friend

import kr.gachon.adigo.data.model.dto.FriendDeleteResponse
import kr.gachon.adigo.data.model.dto.FriendListResponse
import kr.gachon.adigo.data.model.dto.FriendRequestListResponse
import kr.gachon.adigo.data.model.dto.FriendRequestReplyDto
import kr.gachon.adigo.data.model.dto.FriendshipRequestLookupDto
import kr.gachon.adigo.data.model.dto.ProfileResponse
import retrofit2.Response
import retrofit2.http.*

// friend/FriendApi.kt
interface FriendApi {
    @GET("member/friend/list")
    suspend fun friendList(): Response<FriendListResponse>

    @DELETE("member/friend/{friendEmail}")
    suspend fun deleteFriend(@Path("friendEmail") friendEmail: String): Response<FriendDeleteResponse>

    @POST("member/friend/{friendEmail}")
    suspend fun addFriend(@Path("friendEmail") friendEmail: String): Response<ProfileResponse>

    @GET("member/friend/lookup")
    suspend fun getFriendRequests(): Response<FriendRequestListResponse>

    @POST("member/friend/reply")
    suspend fun replyToFriendRequest(@Body dto: FriendRequestReplyDto): Response<ProfileResponse>
}