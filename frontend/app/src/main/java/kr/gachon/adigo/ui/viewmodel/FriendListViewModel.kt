package kr.gachon.adigo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.gachon.adigo.data.local.repository.UserDatabaseRepository
import kr.gachon.adigo.data.local.transformer.UserTransformer
import kr.gachon.adigo.data.remote.ApiService      // GET /friends
import kr.gachon.adigo.data.model.dto.FriendListResponse  // 서버 DTO
import kr.gachon.adigo.data.model.dto.ProfileResponse
import kr.gachon.adigo.data.model.global.User     // 도메인 모델

class FriendListViewModel(
    private val remoteDataSource: ApiService,
    private val repo: UserDatabaseRepository
) : ViewModel() {

    /** UI-계층에서 구독할 Flow */
    val friends = repo.friendsFlow

    /** 서버에서 친구 목록을 받아 DB 에 반영 */
    fun refreshFriends() {
        viewModelScope.launch {
            val res = remoteDataSource.getFriendList()          // suspend fun
            if (res.isSuccessful) {
                val listDto: FriendListResponse? = res.body()
                val entities = listDto?.data
                    ?.map { dto ->
                        val user = User(           // DTO → 도메인
                            email        = dto.email,
                            nickname     = dto.nickname,
                            profileImage = dto.profileImage,
                            authority    = dto.authority
                        )
                        UserTransformer.modelToEntity(user)   // 도메인 → Entity
                    }.orEmpty()

                repo.upsertAll(entities)           // Realm 일괄 저장
            }
            /* 에러 처리 생략 */
        }
    }
}
