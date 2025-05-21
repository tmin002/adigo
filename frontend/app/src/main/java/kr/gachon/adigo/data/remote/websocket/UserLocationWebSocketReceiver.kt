package kr.gachon.adigo.data.remote.websocket

import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    internal var receiverJob: Job? = null

    // Destination for receiving friend locations as per spec
    private val FRIENDS_LOCATION_RESPONSE_DESTINATION = "/user/queue/friendsLocationResponse"

    fun startListening() {
        Log.d(TAG, "Starting UserLocationWebSocketReceiver listening...")

        // Subscribe to the destination via the STOMP client
        stompClient.subscribe(FRIENDS_LOCATION_RESPONSE_DESTINATION)
        Log.d(TAG, "Subscribed to $FRIENDS_LOCATION_RESPONSE_DESTINATION")

        // Collect messages from the client's flow
        receiverJob = stompClient.messageFlow
            .onEach { (destination, body) ->
                Log.d(TAG, "▲ inbound [$destination] $body")   // <-- 꼭 찍어보세요
            }
            .filter { (destination, _) ->
                destination.endsWith("friendsLocationResponse")   // 가장 안전
            }
            .onEach { (_, body) ->
                // onEach 안 자체가 이미 코루틴이므로 launch 필요 없음
                handleFriendsLocationResponse(body)
            }
            .catch { Log.e(TAG, "Error in message flow", it) }
            .launchIn(coroutineScope)

        Log.d(TAG, "UserLocationWebSocketReceiver listening job launched.")
    }

    fun stopListening() {
        Log.d(TAG, "Stopping UserLocationWebSocketReceiver listening...")
        // Unsubscribe from the destination
        stompClient.unsubscribe(FRIENDS_LOCATION_RESPONSE_DESTINATION)
        // Cancel the collecting job
        receiverJob?.cancel()
        receiverJob = null
        Log.d(TAG, "UserLocationWebSocketReceiver listening stopped.")
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