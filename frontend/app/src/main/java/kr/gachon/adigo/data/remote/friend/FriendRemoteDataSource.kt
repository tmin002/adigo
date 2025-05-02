package kr.gachon.adigo.data.remote.friend

import kr.gachon.adigo.data.remote.core.safeCall

class FriendRemoteDataSource  (
    private val api: FriendApi
) {
    suspend fun friendList() = safeCall { api.friendList() }
}