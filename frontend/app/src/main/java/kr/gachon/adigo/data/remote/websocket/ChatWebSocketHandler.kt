package kr.gachon.adigo.data.remote.websocket

import com.google.gson.Gson
import kr.gachon.adigo.data.model.global.UserLocation

class UserLocationWebSocketHandler: AbstractWebSocketReceiveHandler {
    fun makeGson(): Gson = Gson()
    override val receiveActionTable = WebSocketReceiveActionTable(
        mapOf(
            "/user/location" to this::onUserLocationReceive
        )
    )
    fun onUserLocationReceive(frame: String) {
        class DTO {
            inner class Friend {
                lateinit var id: String
                lateinit var location: UserLocation
            }
            lateinit var friends_list: List<Friend>
        }

        //val gson = Gson()
        //val received: DTO =
    }
}