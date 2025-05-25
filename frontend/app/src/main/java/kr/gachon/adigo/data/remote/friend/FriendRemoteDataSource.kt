package kr.gachon.adigo.data.remote.friend

import android.util.Log
import kr.gachon.adigo.data.model.dto.FriendListResponse
import kr.gachon.adigo.data.model.dto.FriendRequestListResponse
import kr.gachon.adigo.data.model.dto.FriendRequestReplyDto
import kr.gachon.adigo.data.model.dto.FriendDeleteResponse
import kr.gachon.adigo.data.model.dto.ProfileResponse
import retrofit2.Response
import java.util.Optional

class FriendRemoteDataSource(
    private val api: FriendApi
) {
    private val TAG = "FriendRemoteDataSource"

    suspend fun friendList(): Response<FriendListResponse> {
        return try {
            Log.d(TAG, "Calling friendList API")
            val response = api.friendList()
            Log.d(TAG, "friendList API response: ${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error in friendList", e)
            throw e
        }
    }

    suspend fun deleteFriend(friendEmail: String): Response<FriendDeleteResponse> {
        return try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteFriend", e)
            throw e
        }
    }

    suspend fun addFriend(friendEmail: String): Response<ProfileResponse> {
        return try {
            Log.d(TAG, "Calling addFriend API for: $friendEmail")
            val response = api.addFriend(friendEmail)
            Log.d(TAG, "addFriend API response: ${response.isSuccessful}")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error in addFriend", e)
            throw e
        }
    }

    suspend fun getFriendRequests(): Response<FriendRequestListResponse> {
        return try {
            Log.d(TAG, "Calling getFriendRequests API")
            val response = api.getFriendRequests()
            Log.d(TAG, "getFriendRequests API response: ${response.isSuccessful}")
            if (response.isSuccessful) {
                Log.d(TAG, "getFriendRequests response body: ${response.body()}")
            } else {
                Log.e(TAG, "getFriendRequests error: ${response.errorBody()?.string()}")
            }
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error in getFriendRequests", e)
            throw e
        }
    }

    suspend fun replyToFriendRequest(dto: FriendRequestReplyDto): Response<ProfileResponse> {
        return try {
            Log.d(TAG, "Calling replyToFriendRequest API for: ${dto.friendEmail}")
            Log.d(TAG, "Request body: ${dto}")
            val response = api.replyToFriendRequest(dto)
            Log.d(TAG, "replyToFriendRequest API response: ${response.isSuccessful}")
            if (!response.isSuccessful) {
                Log.e(TAG, "Error response body: ${response.errorBody()?.string()}")
                Log.e(TAG, "Error code: ${response.code()}")
            }
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error in replyToFriendRequest", e)
            throw e
        }
    }
}