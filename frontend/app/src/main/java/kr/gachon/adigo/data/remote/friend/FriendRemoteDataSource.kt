package kr.gachon.adigo.data.remote.friend

import kr.gachon.adigo.data.remote.core.safeCall
import retrofit2.Response
import retrofit2.http.GET

class FriendRemoteDataSource  (
    private val api: FriendApi
) {
    suspend fun friendList() = safeCall { api.friendList() }
    suspend fun deleteFriend(friendEmail: String) = safeCall { api.deleteFriend(friendEmail) }
    suspend fun addFriend(friendEmail: String) = safeCall { api.addFriend(friendEmail) }

}