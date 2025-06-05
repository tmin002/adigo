package kr.gachon.adigo.data.remote.websocket

import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.gachon.adigo.data.local.repository.UserLocationRepository
import kr.gachon.adigo.data.model.dto.FriendsLocationResponseDto
import kr.gachon.adigo.data.model.global.UserLocationDto // Reusing app's DTO for repository

class UserLocationWebSocketReceiver(
    private val stompClient: StompWebSocketClient,
    private val userLocationRepository: UserLocationRepository,
    private val gson: Gson, // Inject Gson for parsing
    private val coroutineScope: CoroutineScope // Scope for collecting flow and DB ops
) {

    private val TAG = "UserLocationReceiver"
    internal var listenJob: Job? = null



    // Destination for receiving friend locations as per spec
    private val FRIENDS_LOCATION_RESPONSE_DESTINATION = "/user/queue/friendsLocationResponse"

    fun startListening() {
        if (listenJob != null) return               // 이미 시작되어 있으면 무시
        Log.d(TAG, "🔔 startListening")
        listenJob = coroutineScope.launch {
            /** 1️⃣  STOMP 연결 상태 감시 → 연결되면 구독 */
            launch {
                while (isActive) {
                    if (stompClient.stompConnected.value &&
                        !stompClient.isSubscribed(FRIENDS_LOCATION_RESPONSE_DESTINATION))       // ← 확장 함수(아래)로 체크
                    {
                        stompClient.subscribe(FRIENDS_LOCATION_RESPONSE_DESTINATION)
                        Log.i(TAG, "SUBSCRIBE sent for $FRIENDS_LOCATION_RESPONSE_DESTINATION")

                    }
                    delay(500)  // 가벼운 폴링
                }
            }

            /** 2️⃣  메시지 스트림 수집 */
            stompClient.messageFlow
                .filter { (d, _) -> d == FRIENDS_LOCATION_RESPONSE_DESTINATION }
                .onEach { (_, body) -> handleFriendsLocationResponse(body) }
                .catch  { Log.e(TAG, "Flow error", it) }
                .launchIn(this)
        }
    }

    fun stopListening() {
        Log.d(TAG, "🛑 stopListening")
        stompClient.unsubscribe(FRIENDS_LOCATION_RESPONSE_DESTINATION)
        listenJob?.cancel()
        listenJob = null
    }

    private suspend fun handleFriendsLocationResponse(jsonBody: String) {
        try {
            // Parse the JSON body into the DTO
            //val responseDto = gson.fromJson(jsonBody, FriendsLocationResponseDto::class.java)
            val listType = object : TypeToken<List<UserLocationDto>>() {}.type
            val friendDtos: List<UserLocationDto> = gson.fromJson(jsonBody, listType)
            Log.v(TAG, "Parsed FriendsLocationResponseDto: $friendDtos")

            // Map the server's DTO list (FriendLocationDto) to the app's repository DTO list (UserLocationDto)
            val userLocationDtos = friendDtos.map { friendDto ->
                UserLocationDto(
                    id = friendDto.id,
                    lat = friendDto.lat,
                    lng = friendDto.lng
                )
            }

            // Upsert the locations into the local database (on IO dispatcher)
            withContext(Dispatchers.IO) {
                userLocationRepository.upsert(userLocationDtos)
                Log.d(TAG, "Upserted ${userLocationDtos.size} friend locations to DB.")
            }

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse JSON for friend locations: $jsonBody", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing friend location message", e)
        }
    }

} 