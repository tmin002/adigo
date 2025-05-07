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

class FriendListViewModel(
    private val repo: UserDatabaseRepository
) : ViewModel() {

    /** UI-계층에서 구독할 Flow */
    val friends = repo.friendsFlow
    private val remoteDataSource = AdigoApplication.friendRemote

    /** 서버에서 친구 목록을 받아 DB 에 반영 */
    fun refreshFriends() {
        viewModelScope.launch {

            val result = remoteDataSource.friendList()
            result.onSuccess {
                val listDto: FriendListResponse? = result.getOrNull()
                val entities = listDto?.data
                    ?.map { dto ->
                        val user = User(           // DTO → 도메인
                            email = dto.email,
                            nickname = dto.nickname,
                            profileImage = dto.profileImage,
                            authority = dto.authority
                        )
                        UserTransformer.modelToEntity(user)   // 도메인 → Entity
                    }.orEmpty()

                repo.upsertAll(entities)           // Realm 일괄 저장
            }
                .onFailure {

                }

        }
    }


    fun deleteFriend(friend: UserEntity) {
        viewModelScope.launch {
            val result = remoteDataSource.deleteFriend(friend.email)
            repo.delete(friend.id)
            result.onSuccess {

            }
        }
    }
}
