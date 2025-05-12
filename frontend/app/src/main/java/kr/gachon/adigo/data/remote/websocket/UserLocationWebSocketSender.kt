package kr.gachon.adigo.data.remote.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kr.gachon.adigo.data.model.dto.LocationUpdateDto

class UserLocationWebSocketSender(
    private val stompClient: StompWebSocketClient,
    private val gson: Gson // Inject Gson for sending
) {

    private val TAG = "UserLocationSender"

    // Destinations for sending messages as per spec
    private val LOCATION_UPDATE_DESTINATION = "/app/location/update"
    private val REQUEST_FRIENDS_DESTINATION = "/app/location/requestFriends"

    /**
     * Sends the current user's location to the server.
     */
    fun sendMyLocation(latitude: Double, longitude: Double) {
        val locationDto = LocationUpdateDto(latitude = latitude, longitude = longitude)
        val jsonBody = gson.toJson(locationDto)

        Log.d(TAG, "Sending my location: $jsonBody")
        stompClient.send(LOCATION_UPDATE_DESTINATION, jsonBody)
    }

    /**
     * Requests the current locations of the user's friends from the server.
     */
    fun requestFriendLocations() {
        // Spec says example body is "{}"
        val jsonBody = "{}" // Or gson.toJson(JsonObject()) if using Gson
        Log.d(TAG, "Requesting friend locations...")
        stompClient.send(REQUEST_FRIENDS_DESTINATION, jsonBody)
    }
} 