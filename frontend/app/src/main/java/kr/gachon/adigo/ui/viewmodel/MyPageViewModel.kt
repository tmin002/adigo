package kr.gachon.adigo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.local.repository.UserDatabaseRepository
import kr.gachon.adigo.data.local.transformer.UserTransformer
import kr.gachon.adigo.data.model.dto.FriendListResponse  // 서버 DTO
import kr.gachon.adigo.data.model.dto.ProfileResponse
import kr.gachon.adigo.data.model.global.User     // 도메인 모델
import kr.gachon.adigo.data.remote.friend.FriendApi

class MyPageViewModel(
    private val repo: UserDatabaseRepository
) : ViewModel() {
}
