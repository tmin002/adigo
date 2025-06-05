package kr.gachon.adigo.data.remote.websocket

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.model.dto.RefreshTokenRequest
import kr.gachon.adigo.data.remote.auth.AuthRemoteDataSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class StompWebSocketClient(
    private val websocketUrl: String, // e.g., "ws://adigo.site/ws-stomp" (SockJS endpoint)
    private val tokenManager: TokenManager,
    private val authRemote: AuthRemoteDataSource,
    private val applicationScope: CoroutineScope // Scope for client's operations
) {

    private val TAG = "StompWebSocketClient"
    private val defaultDestinations = listOf("/user/queue/friendsLocationResponse")

    private var webSocket: WebSocket? = null


    private val _stompConnected = MutableStateFlow(false)
    val stompConnected: StateFlow<Boolean> = _stompConnected.asStateFlow()


    private var connectionJob: Job? = null

    // Flow to emit received STOMP messages (destination, body)
    private val _messageFlow = MutableSharedFlow<Pair<String, String>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow: SharedFlow<Pair<String, String>> = _messageFlow.asSharedFlow()

    // Registered subscription handlers (destination -> subscriptionId)
    private val subscriptions = mutableMapOf<String, String>()

    private val wsOkHttpClient: OkHttpClient by lazy {
        // Create a new OkHttpClient instance without the Authorization interceptor
        OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun isSubscribed(dest: String): Boolean =
        subscriptions.containsKey(dest)

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened: ${response.code}")
            // Send STOMP CONNECT frame after raw WebSocket is open
            sendConnectFrame()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.v(TAG, "Raw WebSocket message received: $text")
            handleStompFrame(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code / $reason")
            _stompConnected.value = false
            this@StompWebSocketClient.webSocket = null
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code / $reason")
            _stompConnected.value = false
            // Attempt to reconnect after a delay
            startReconnecting()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${response?.code} / ${response?.message}", t)

            if (t is java.net.SocketException) {
                Log.e(TAG, "Detected SocketException. Resetting WebSocket object.")
            }

            _stompConnected.value = false
            this@StompWebSocketClient.webSocket = null // <- 여기가 핵심
            startReconnecting()
        }
    }

    private fun sendConnectFrame() {
        val token = tokenManager.getJwtToken() ?: "" // Get current token
        val connectFrame = buildString {
            append("CONNECT\n")
            append("accept-version:1.1,1.2\n")
            append("heart-beat:10000,10000\n") // Use heartbeat as per spec
            append("Authorization:Bearer $token\n") // Include JWT header
            append("\n\u0000")
        }
        Log.i("WEBSOCKET TEST", connectFrame)
        webSocket?.send(connectFrame) ?: Log.w(TAG, "WebSocket is null, cannot send CONNECT")
        Log.d(TAG, "Sent STOMP CONNECT frame")
    }

    private fun handleStompFrame(frame: String) {
        // Split frame into headers and body
        Log.d(TAG, "⚡ FRAME >>> $frame")
        val parts = frame.split("\n\n", limit = 2)
        val headersPart = parts.getOrNull(0) ?: return // Malformed frame
        val bodyPart = parts.getOrNull(1)?.trimEnd('\u0000') ?: ""

        val headerLines = headersPart.split("\n")
        val command = headerLines.getOrNull(0) ?: return // Malformed frame

        val headers = mutableMapOf<String, String>()
        headerLines.drop(1).forEach { line ->
            val headerParts = line.split(":", limit = 2)
            if (headerParts.size == 2) {
                headers[headerParts[0].trim()] = headerParts[1].trim()
            }
        }

        Log.d(TAG, "Received STOMP command: $command")
        Log.v(TAG, "Headers: $headers")
        Log.v(TAG, "Body: $bodyPart")

        when (command) {
            "CONNECTED" -> {
                _stompConnected.value = true
                Log.i(TAG, "STOMP CONNECTED. Session: ${headers["session"]}")
                defaultDestinations.forEach { dest ->
                    if(!subscriptions.contains(dest)){
                        subscribe(dest)
                    }
                }
                resubscribeOnConnect()
            }
            "MESSAGE" -> {
                val destination = headers["destination"]
                if (destination != null) {
                    // Emit the received message to the Flow
                    applicationScope.launch {
                        _messageFlow.emit(destination to bodyPart)
                    }
                } else {
                    Log.w(TAG, "Received MESSAGE without destination header")
                }
            }
            "ERROR" -> {
                val msg = headers["message"] ?: ""
                Log.e(TAG, "STOMP ERROR: $msg\nBody:\n$bodyPart")

                // 🔑 토큰 만료로 추정되는 키워드(예: 'Expired', 'Invalid')가 오면 갱신 시도
                if (msg.contains("expired", true) || msg.contains("Invalid", true)) {
                    applicationScope.launch {
                        if (refreshToken()) {          // refresh 성공하면
                            reconnectWithNewToken()    // 웹소켓 재연결
                        } else {
                            // refresh 실패 → 로그인 만료 처리 등을 UI 쪽에 통보
                        }
                    }
                } else {
                    // 기타 오류는 기존 로직(재연결 or 종료)으로 진행
                    startReconnecting()
                }
            }
            "RECEIPT" -> {
                val receiptId = headers["receipt-id"]
                Log.d(TAG, "Received RECEIPT for id: $receiptId")
            }
            // Handle other STOMP commands like HEARTBEAT if necessary (okhttp handles raw pings)
            else -> {
                Log.d(TAG, "Received unhandled STOMP command: $command")
            }
        }
    }

    private fun startReconnecting() {
        connectionJob?.cancel() // Cancel any existing connection job
        connectionJob = applicationScope.launch {
            var delayTime = 1000L // Start with 1 second delay
            val maxDelay = 30000L // Maximum 30 seconds delay

            while (isActive && !stompConnected.value) {
                Log.d(TAG, "Attempting to reconnect in ${delayTime / 1000} seconds...")
                delay(delayTime)
                connect() // Attempt to connect

                // Increase delay time exponentially, up to maxDelay
                delayTime = (delayTime * 2).coerceAtMost(maxDelay)
            }
        }
    }

    private fun resubscribeOnConnect() {
        subscriptions.toMap().forEach { (destination, subId) ->
            val subscribeFrame = buildString {
                append("SUBSCRIBE\n")
                append("id:$subId\n")
                append("destination:$destination\n\n\u0000")
            }
            webSocket?.send(subscribeFrame)
        }
    }

    fun connect() {
        if (webSocket != null) {
            Log.d(TAG, "WebSocket client already exists.")
            if (!stompConnected.value) {
                startReconnecting()
            }
            return
        }

        val request = Request.Builder()
            .url(websocketUrl)
            .build()

        try {
            // Use the custom wsOkHttpClient without the Authorization interceptor
            webSocket = wsOkHttpClient.newWebSocket(request, webSocketListener)
            Log.d(TAG, "WebSocket connection attempt started.")
            Log.d(TAG, AdigoApplication.AppContainer.tokenManager.getJwtToken()!!)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebSocket", e)
            startReconnecting()
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting STOMP client")
        connectionJob?.cancel() // Cancel reconnection job
        _stompConnected.value = false

        val disconnectFrame = "DISCONNECT\n\n\u0000"
        webSocket?.send(disconnectFrame) // Send STOMP DISCONNECT frame

        // Close the underlying WebSocket immediately
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null // Clear reference
        Log.i(TAG, "WebSocket client disconnected.")
    }

    fun subscribe(destination: String): String {
        if (subscriptions.contains(destination)) {
             Log.w(TAG, "Already subscribed to $destination")
             return subscriptions[destination]!! // Return existing ID
        }

        val subscriptionId = UUID.randomUUID().toString()
        subscriptions[destination] = subscriptionId

        if (stompConnected.value) {
            val subscribeFrame = buildString {
                append("SUBSCRIBE\n")
                append("id:$subscriptionId\n")
                append("destination:$destination\n")
                append("\n\u0000")
            }
            Log.i("WEBSOCKET TEST", subscribeFrame)
            webSocket?.send(subscribeFrame) ?: Log.w(TAG, "WebSocket is null, cannot send SUBSCRIBE")
            Log.d(TAG, "Sent STOMP SUBSCRIBE frame for $destination with ID $subscriptionId")
        } else {
            Log.w(TAG, "STOMP not connected, SUBSCRIBE for $destination will be sent on connect")
            // The resubscribeOnConnect logic will handle sending this later
        }
        return subscriptionId
    }

    fun unsubscribe(destination: String) {
        val subscriptionId = subscriptions.remove(destination)
        if (subscriptionId != null) {
            if (stompConnected.value) {
                val unsubscribeFrame = buildString {
                    append("UNSUBSCRIBE\n")
                    append("id:$subscriptionId\n")
                    append("\n\u0000")
                }
                Log.i("WEBSOCKET TEST", unsubscribeFrame)
                webSocket?.send(unsubscribeFrame) ?: Log.w(TAG, "WebSocket is null, cannot send UNSUBSCRIBE")
                Log.d(TAG, "Sent STOMP UNSUBSCRIBE frame for $destination (ID $subscriptionId)")
            } else {
                 Log.w(TAG, "STOMP not connected, skipping UNSUBSCRIBE frame for $destination")
            }
        } else {
            Log.w(TAG, "Not subscribed to $destination")
        }
    }

    fun send(destination: String, body: String, contentType: String = "application/json") {
        if (!stompConnected.value) {
            Log.w(TAG, "STOMP not connected, cannot send message to $destination")
            return
        }

        val sendFrame = buildString {
            append("SEND\n")
            append("destination:$destination\n")
            append("content-type:$contentType\n")
            append("content-length:${body.toByteArray().size}\n") // Important for binary/text
            append("\n") // Empty line separates headers from body
            append(body)
            append("\u0000") // Null terminator
        }
        Log.i("WEBSOCKET", sendFrame)
        webSocket?.send(sendFrame) ?: Log.w(TAG, "WebSocket is null, cannot send message to $destination")
        Log.d(TAG, "Sent STOMP SEND frame to $destination with body: $body")
    }

    // ──────────────────────────────────────────────
    // ▶ refreshToken(): suspend fun
    //    refreshToken 호출 성공 시 true, 실패 시 false
    // ──────────────────────────────────────────────
    private suspend fun refreshToken(): Boolean {
        val refresh = tokenManager.getRefreshToken() ?: return false
        return runCatching {
            authRemote.refresh(
                RefreshTokenRequest(
                    accessToken  = tokenManager.getJwtToken() ?: "",
                    refreshToken = refresh
                )
            ).getOrNull()?.data
        }.getOrNull()?.let {
            tokenManager.saveTokens(it)
            Log.i(TAG, "🎫 AccessToken refreshed!")
            true
        } ?: false
    }

    // ──────────────────────────────────────────────
    // ▶ 재연결 : 기존 웹소켓을 닫고 새로 connect()
    // ──────────────────────────────────────────────
    private fun reconnectWithNewToken() {
        _stompConnected.value = false
        webSocket?.close(1000, "refresh token done")
        webSocket = null
        connect()          // → 내부에서 sendConnectFrame()을 호출하며
        //    새 accessToken이 자동으로 실림
    }


    // Lifecycle method to clean up coroutine scope
    fun shutdown() {
        disconnect() // Ensure disconnect is called
        applicationScope.cancel() // Cancel the scope
        Log.d(TAG, "StompWebSocketClient shut down.")
    }
} 