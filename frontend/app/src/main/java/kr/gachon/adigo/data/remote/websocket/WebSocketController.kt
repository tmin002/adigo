package kr.gachon.adigo.data.remote.websocket
import com.google.gson.Gson
import kr.gachon.adigo.AdigoApplication
import okhttp3.*
import java.util.concurrent.TimeUnit
import kr.gachon.adigo.R

object WebSocketManager {
    private lateinit var webSocket: WebSocket
    private var client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect(onMessageReceived: (String) -> Unit) {
        val jwtToken = AdigoApplication.tokenManager.getJwtToken()
        val request = Request.Builder()
            .url("${R.string.server_address}${R.string.stomp_url}")
            .addHeader("Authorization", "Bearer $jwtToken") // CONNECT 프레임에 포함되지는 않음
            .build()
        webSocket = client.newWebSocket(request, StompWebSocketListener())
    }

    fun subscribe(destination: String) {
        val subscribeFrame = buildString {
            append("SUBSCRIBE\n")
            append("id:sub-0\n")
            append("destination:$destination\n")
            append("\n\u0000")
        }
        webSocket?.send(subscribeFrame)
    }

    fun send(destination: String, body: Any) {
        val gson = Gson()
        val sendFrame = buildString {
            append("SEND\n")
            append("destination:$destination\n")
            append("content-type:application/json\n")
            append("\n")
            append(gson.toJson(body))
            append("\u0000")
        }
        webSocket?.send(sendFrame)
    }

    fun disconnect() {
        val disconnectFrame = "DISCONNECT\nreceipt:77\n\n\u0000"
        webSocket?.send(disconnectFrame)
        webSocket?.close(1000, "Client disconnected")
    }
}