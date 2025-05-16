package kr.gachon.adigo.data.local.repository

import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kr.gachon.adigo.data.local.entity.UserLocationEntity
import kr.gachon.adigo.data.local.transformer.UserLocationTransformer
import kr.gachon.adigo.data.model.global.UserLocation
import kr.gachon.adigo.data.model.global.UserLocationDto
import io.realm.kotlin.ext.query

class UserLocationRepository(private val realm: Realm) {

    /** 실시간 스트림 → Flow<List<UserLocation>> */
    fun locationsFlow(): Flow<List<UserLocationEntity>> =
        realm.query<UserLocationEntity>(UserLocationEntity::class)
            .asFlow()                      // RealmChange<UserLocation>
            .map { results -> results.list }
            .flowOn(Dispatchers.IO)

    /** WebSocket 서비스가 호출 */
    suspend fun upsert(list: List<UserLocationDto>) {
        realm.write {
            list.forEach { dto ->
                copyToRealm(
                    UserLocationTransformer.modelToEntity(UserLocation(dto.id, dto.lat, dto.lng)),
                    UpdatePolicy.ALL
                )
            }
        }
    }

    /** 친구 위치 목록을 UserLocationDto로 변환하여 Flow로 노출 */
    val friends: Flow<List<UserLocationDto>> =
        locationsFlow().map { list ->
            list.map { entity ->
                UserLocationDto(
                    id = entity.id,
                    lat = entity.lat,
                    lng = entity.lng
                )
            }
        }.flowOn(Dispatchers.IO)

    /** id로 위치 정보 삭제 */
    suspend fun deleteById(id: Long) {
        realm.write {
            val entity = this.query<UserLocationEntity>("id == $0", id).first().find()
            entity?.let { delete(it) }
        }
    }
}