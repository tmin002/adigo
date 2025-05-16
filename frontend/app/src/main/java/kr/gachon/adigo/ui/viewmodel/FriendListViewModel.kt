package kr.gachon.adigo.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.local.repository.UserDatabaseRepository
import kr.gachon.adigo.data.local.transformer.UserTransformer
import kr.gachon.adigo.data.model.dto.FriendListResponse  // 서버 DTO
import kr.gachon.adigo.data.model.dto.FriendRequestReplyDto
import kr.gachon.adigo.data.model.dto.FriendshipRequestLookupDto
import kr.gachon.adigo.data.model.global.User     // 도메인 모델
import kr.gachon.adigo.data.remote.friend.FriendApi

class FriendListViewModel(
    private val repo: UserDatabaseRepository
) : ViewModel() {

    private val TAG = "FriendListViewModel"

    /** UI-계층에서 구독할 Flow */
    val friends = repo.friendsFlow
    private val remoteDataSource = AdigoApplication.AppContainer.friendRemote

    private val _friendRequests = MutableStateFlow<List<FriendshipRequestLookupDto>>(emptyList())
    val friendRequests: StateFlow<List<FriendshipRequestLookupDto>> = _friendRequests

    /** 서버에서 친구 목록을 받아 DB 에 반영 */
    fun refreshFriends() {
        viewModelScope.launch {
            Log.d(TAG, "Starting to refresh friends list")
            val result = remoteDataSource.friendList()
            result.onSuccess {
                Log.d(TAG, "Successfully got friends list")
                val listDto: FriendListResponse? = result.getOrNull()
                val entities = listDto?.data
                    ?.map { dto ->
                        val user = User(
                            id = dto.id,
                            email = dto.email,
                            nickname = dto.nickname,
                            profileImage = dto.profileImage,
                            authority = dto.authority
                        )
                        UserTransformer.modelToEntity(user)   // 도메인 → Entity
                    }.orEmpty()

                repo.upsertAll(entities)           // Realm 일괄 저장
                Log.d(TAG, "Updated friends in local database")
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to refresh friends list", error)
            }
        }
    }

    fun refreshFriendRequests() {
        viewModelScope.launch {
            Log.d(TAG, "Starting to refresh friend requests")
            try {
                val result = remoteDataSource.getFriendRequests()
                result.onSuccess { response ->
                    Log.d(TAG, "Successfully got friend requests: ${response}")
                    _friendRequests.value = response.data
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to get friend requests", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while getting friend requests", e)
            }
        }
    }

    fun deleteFriend(friend: UserEntity) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting friend: ${friend.email}")
            val result = remoteDataSource.deleteFriend(friend.email)
            result.onSuccess { response ->
                Log.d(TAG, "Successfully deleted friend, response: $response")
                if (response.status == 200) {
                    repo.delete(friend.id.toString())
                    AdigoApplication.AppContainer.userLocationRepo.deleteById(friend.id)
                } else {
                    Log.e(TAG, "Failed to delete friend: ${response.message}")
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to delete friend", error)
            }
        }
    }

    fun addFriend(email: String) {
        viewModelScope.launch {
            Log.d(TAG, "Adding friend: $email")
            val result = remoteDataSource.addFriend(email)
            result.onSuccess {
                Log.d(TAG, "Successfully added friend")
                refreshFriends()
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to add friend", error)
            }
        }
    }

    fun replyToFriendRequest(friendEmail: String, accept: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Replying to friend request from $friendEmail, accept: $accept")
            val dto = FriendRequestReplyDto(friendEmail = friendEmail, isAccepted = accept)
            val result = remoteDataSource.replyToFriendRequest(dto)
            result.onSuccess {
                Log.d(TAG, "Successfully replied to friend request")
                refreshFriendRequests()
                if (accept) {
                    refreshFriends()
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to reply to friend request", error)
            }
        }
    }

    /** 푸시 알림을 받았을 때 호출할 메서드 */
    fun onFriendRequestNotificationReceived() {
        Log.d(TAG, "Friend request notification received, refreshing friend requests")
        refreshFriendRequests()
    }
}
