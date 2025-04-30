package kr.gachon.adigo.data.local.repository

import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kr.adigo.adigo.database.entity.UserEntity

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
    suspend fun upsertAll(users: List<UserEntity>) {
        realm.write {
            users.forEach { entity ->
                copyToRealm(entity)
            }
        }
    }

    suspend fun upsert(user: UserEntity) = upsertAll(listOf(user))

    suspend fun delete(userId: String) {
        realm.write {
            query<UserEntity>("id == $0", userId)
                .first()
                .find()
                ?.let { delete(it) }
        }
    }
}
