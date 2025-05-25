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
import kr.gachon.adigo.data.model.dto.FriendListResponse
import kr.gachon.adigo.data.model.dto.FriendRequestListResponse
import kr.gachon.adigo.data.model.dto.FriendRequestReplyDto
import kr.gachon.adigo.data.model.dto.FriendshipRequestLookupDto
import kr.gachon.adigo.data.model.global.User
import kr.gachon.adigo.data.remote.friend.FriendApi
import retrofit2.Response

class FriendListViewModel(
    private val repo: UserDatabaseRepository
) : ViewModel() {

    private val TAG = "FriendListViewModel"

    /** UI-계층에서 구독할 Flow */
    private val _friends = MutableStateFlow<List<UserEntity>>(emptyList())
    val friends: StateFlow<List<UserEntity>> = _friends

    private val _friendRequests = MutableStateFlow<List<FriendshipRequestLookupDto>>(emptyList())
    val friendRequests: StateFlow<List<FriendshipRequestLookupDto>> = _friendRequests

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val response = AdigoApplication.AppContainer.userRemote.getCurrentUser()
                if (response.isSuccessful) {
                    response.body()?.data?.let { user ->
                        val userEntity = UserTransformer.modelToEntity(user)
                        repo.upsert(userEntity)
                        _currentUser.value = userEntity
                    }
                }
            } catch (e: Exception) {
                Log.e("FriendListViewModel", "Failed to load current user", e)
            }
        }
    }

    /** 서버에서 친구 목록을 받아 DB 에 반영 */
    fun refreshFriends() {
        viewModelScope.launch {
            try {
                val response: Response<FriendListResponse> = AdigoApplication.AppContainer.friendRemote.friendList()
                if (response.isSuccessful) {
                    response.body()?.let { friendListResponse: FriendListResponse ->
                        friendListResponse.data.forEach { friend ->
                            repo.upsert(UserTransformer.modelToEntity(friend))
                        }
                        // 현재 사용자를 제외한 친구 목록만 표시
                        _friends.value = friendListResponse.data
                            .map { UserTransformer.modelToEntity(it) }
                            .filter { it.id != _currentUser.value?.id }
                    }
                }
            } catch (e: Exception) {
                Log.e("FriendListViewModel", "Failed to refresh friends", e)
            }
        }
    }

    fun refreshFriendRequests() {
        viewModelScope.launch {
            try {
                val response: Response<FriendRequestListResponse> = AdigoApplication.AppContainer.friendRemote.getFriendRequests()
                if (response.isSuccessful) {
                    response.body()?.let { requestListResponse: FriendRequestListResponse ->
                        _friendRequests.value = requestListResponse.data
                    }
                }
            } catch (e: Exception) {
                Log.e("FriendListViewModel", "Failed to refresh friend requests", e)
            }
        }
    }

    fun deleteFriend(friend: UserEntity) {
        viewModelScope.launch {
            try {
                val response = AdigoApplication.AppContainer.friendRemote.deleteFriend(friend.email)
                if (response.isSuccessful) {
                    refreshFriends()
                }
            } catch (e: Exception) {
                Log.e("FriendListViewModel", "Failed to delete friend", e)
            }
        }
    }

    fun addFriend(email: String) {
        viewModelScope.launch {
            try {
                val response = AdigoApplication.AppContainer.friendRemote.addFriend(email)
                if (response.isSuccessful) {
                    refreshFriends()
                }
            } catch (e: Exception) {
                Log.e("FriendListViewModel", "Failed to add friend", e)
            }
        }
    }

    fun replyToFriendRequest(requesterEmail: String, accept: Boolean) {
        viewModelScope.launch {
            try {
                val response = AdigoApplication.AppContainer.friendRemote.replyToFriendRequest(
                    FriendRequestReplyDto(requesterEmail, accept)
                )
                if (response.isSuccessful) {
                    refreshFriendRequests()
                    if (accept) {
                        refreshFriends()
                    }
                }
            } catch (e: Exception) {
                Log.e("FriendListViewModel", "Failed to reply to friend request", e)
            }
        }
    }

    /** 푸시 알림을 받았을 때 호출할 메서드 */
    fun onFriendRequestNotificationReceived() {
        Log.d(TAG, "Friend request notification received, refreshing friend requests")
        refreshFriendRequests()
    }
}
