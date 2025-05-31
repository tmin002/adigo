package kr.gachon.adigo.data.local.repository

import android.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.local.transformer.UserTransformer
import kr.gachon.adigo.data.model.dto.FriendListResponse
import retrofit2.Response
import kotlin.collections.filter
import kotlin.collections.map

class UserDatabaseRepository(private val realm: Realm) {

    /** 친구 전체 실시간 구독 */
    val friendsFlow: Flow<List<UserEntity>> =
        realm.query<UserEntity>()
            .asFlow()                                   // Flow<ResultsChange<UserEntity>>
            .map { change: ResultsChange<UserEntity> ->
                change.list                             // List<UserEntity>
            }
            .flowOn(Dispatchers.IO)

    /** 단일 · 일괄 upsert 모두 지원 */
    private suspend fun upsertAll(users: List<UserEntity>) {
        realm.write {
            users.forEach { entity ->
                copyToRealm(entity, UpdatePolicy.ALL)
            }
        }
    }

    private suspend fun upsert(user: UserEntity) = upsertAll(listOf(user))

    suspend fun delete(id: Long) {
        realm.write {
            query<UserEntity>("id == $0", id)
                .first()
                .find()
                ?.let { delete(it) }
        }
    }

    /** Get user by ID */
    fun getUserById(id: Long): UserEntity? {
        return realm.query<UserEntity>("id == $0", id).first().find()
    }

    suspend fun updateFriendsFromServer() {
        try {
            val response: Response<FriendListResponse> = AdigoApplication.AppContainer.friendRemote.friendList()
            if (response.isSuccessful) {
                response.body()?.let { friendListResponse: FriendListResponse ->
                    val serverFriends =
                        friendListResponse.data.map { UserTransformer.modelToEntity(it) }
                    val serverIds = serverFriends.map { it.id }.toSet()

                    realm.write {
                        // 1. 서버로부터 받은 친구들 upsert
                        serverFriends.forEach { friend ->
                            copyToRealm(friend, UpdatePolicy.ALL)
                        }

                        // 2. 서버에 없는 친구들 삭제
                        val localFriends = query<UserEntity>().find()
                        val toDelete = localFriends.filter { it.id !in serverIds }
                        toDelete.forEach { delete(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FriendListViewModel", "Failed to refresh friends", e)
        }
    }
}
