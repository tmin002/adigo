package kr.gachon.adigo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kr.gachon.adigo.data.local.entity.UserLocationEntity
import kr.gachon.adigo.data.local.repository.FriendLocationInfo
import kr.gachon.adigo.data.local.repository.UserLocationRepository

class FriendLocationViewModel(
    private val repo: UserLocationRepository
) : ViewModel() {

    val friends: StateFlow<List<UserLocationEntity>> =
        repo.locationsFlow()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val friendsWithDistance: StateFlow<List<FriendLocationInfo>> =
        repo.getFriendsLocationInfo()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )
}