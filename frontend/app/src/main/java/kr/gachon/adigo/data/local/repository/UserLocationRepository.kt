package kr.gachon.adigo.data.local.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kr.gachon.adigo.data.local.entity.UserLocationEntity
import kr.gachon.adigo.data.local.transformer.UserLocationTransformer
import kr.gachon.adigo.data.model.global.UserLocation
import kr.gachon.adigo.data.model.global.UserLocationDto
import io.realm.kotlin.ext.query
import kotlin.math.*

data class FriendLocationInfo(
    val id: Long,
    val lat: Double,
    val lng: Double,
    val distance: Double, // 미터 단위
    val bearing: Double  // 도 단위 (0-360)
)

class UserLocationRepository(
    private val realm: Realm,
    private val context: Context
) {
    private val TAG = "UserLocation"
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

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

    /** 현재 사용자의 위치 정보를 업데이트 */
    fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location
    }

    /** 친구들의 위치 정보를 거리와 방향 정보와 함께 가져옴 */
    fun getFriendsLocationInfo(): Flow<List<FriendLocationInfo>> {
        return locationsFlow().map { locations ->
            val currentLocation = _currentLocation.value ?: return@map emptyList()
            
            locations.map { friendLocation ->
                val distance = calculateDistance(
                    currentLocation.latitude, currentLocation.longitude,
                    friendLocation.lat, friendLocation.lng
                )
                val bearing = calculateBearing(
                    currentLocation.latitude, currentLocation.longitude,
                    friendLocation.lat, friendLocation.lng
                )
                
                // 로그 출력
                Log.d(TAG, """
                    친구 ID: ${friendLocation.id}
                    거리: ${String.format("%.2f", distance)}m
                    방향: ${String.format("%.1f", bearing)}°
                    위도: ${friendLocation.lat}
                    경도: ${friendLocation.lng}
                    ----------------------
                """.trimIndent())
                
                FriendLocationInfo(
                    id = friendLocation.id,
                    lat = friendLocation.lat,
                    lng = friendLocation.lng,
                    distance = distance,
                    bearing = bearing
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    companion object {
        private const val EARTH_RADIUS = 6371000.0 // 지구 반경 (미터)

        /** 두 지점 간의 거리를 계산 (미터 단위) */
        private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            
            return EARTH_RADIUS * c
        }

        /** 두 지점 간의 방향을 계산 (도 단위, 0-360) */
        private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLon = Math.toRadians(lon2 - lon1)
            
            val y = sin(dLon) * cos(Math.toRadians(lat2))
            val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                    sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
            
            var bearing = Math.toDegrees(atan2(y, x))
            if (bearing < 0) {
                bearing += 360.0
            }
            
            return bearing
        }
    }
}