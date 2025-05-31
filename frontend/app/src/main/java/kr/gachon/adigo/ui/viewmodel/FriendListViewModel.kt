package kr.gachon.adigo.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
) : ViewModel() {

    private val TAG = "FriendListViewModel"

    val repo = AdigoApplication.AppContainer.userDatabaseRepo
    val locationRepo = AdigoApplication.AppContainer.userLocationRepo

    /** UI-계층에서 구독할 Flow */
    /** 실시간 DB 친구 목록 observe */
    val friends: StateFlow<List<UserEntity>> =
        repo.friendsFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _friendRequests = MutableStateFlow<List<FriendshipRequestLookupDto>>(emptyList())
    val friendRequests: StateFlow<List<FriendshipRequestLookupDto>> = _friendRequests





    /** 서버에서 친구 목록을 받아 DB 에 반영 */
    fun refreshFriends() {
        viewModelScope.launch {
            repo.updateFriendsFromServer()
        }
    }


    // 친구 요청 목록을 받는것
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
                    repo.delete(friend.id)
                    locationRepo.deleteById(friend.id)
                    repo.updateFriendsFromServer()
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

}
