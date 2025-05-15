package kr.gachon.adigo.data.remote.friend

import android.util.Log
import kr.gachon.adigo.data.model.dto.FriendRequestReplyDto
import kr.gachon.adigo.data.remote.core.safeCall
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Optional
import kr.gachon.adigo.AdigoApplication

class FriendRemoteDataSource  (
    private val api: FriendApi
) {
    private val TAG = "FriendRemoteDataSource"

    suspend fun friendList() = safeCall {
        Log.d(TAG, "Calling friendList API")
        val response = api.friendList()
        Log.d(TAG, "friendList API response: ${response.isSuccessful}")
        response
    }

    suspend fun deleteFriend(friendEmail: String) = safeCall {
        Log.d(TAG, "Calling deleteFriend API for: $friendEmail")
        val response = api.deleteFriend(friendEmail)
        Log.d(TAG, "deleteFriend API response: ${response.isSuccessful}")
        if (!response.isSuccessful) {
            Log.e(TAG, "Error response body: ${response.errorBody()?.string()}")
            Log.e(TAG, "Error code: ${response.code()}")
        } else {
            Log.d(TAG, "Success response body: ${response.body()}")
        }
        response
    }

    suspend fun addFriend(friendEmail: String) = safeCall {
        Log.d(TAG, "Calling addFriend API for: $friendEmail")
        val response = api.addFriend(friendEmail)
        Log.d(TAG, "addFriend API response: ${response.isSuccessful}")
        response
    }

    suspend fun getFriendRequests() = safeCall {
        Log.d(TAG, "Calling getFriendRequests API")
        val response = api.getFriendRequests()
        Log.d(TAG, "getFriendRequests API response: ${response.isSuccessful}")
        if (response.isSuccessful) {
            Log.d(TAG, "getFriendRequests response body: ${response.body()}")
        } else {
            Log.e(TAG, "getFriendRequests error: ${response.errorBody()?.string()}")
        }
        response
    }

    suspend fun replyToFriendRequest(dto: FriendRequestReplyDto) = safeCall {
        Log.d(TAG, "Calling replyToFriendRequest API for: ${dto.friendEmail}")
        Log.d(TAG, "Request body: ${dto}")
        val response = api.replyToFriendRequest(dto)
        Log.d(TAG, "replyToFriendRequest API response: ${response.isSuccessful}")
        if (!response.isSuccessful) {
            Log.e(TAG, "Error response body: ${response.errorBody()?.string()}")
            Log.e(TAG, "Error code: ${response.code()}")
        }
        response
    }


}