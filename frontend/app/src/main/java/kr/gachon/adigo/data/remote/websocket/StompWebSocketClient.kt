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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.local.TokenManager
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
    private val okHttpClient: OkHttpClient, // Use the same client as Retrofit
    private val applicationScope: CoroutineScope // Scope for client's operations
) {

    private val TAG = "StompWebSocketClient"

    private var webSocket: WebSocket? = null
    var stompConnected = false
        private set
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

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened: ${response.code}")
            // Send STOMP CONNECT frame after raw WebSocket is open
            sendConnectFrame()
            // Re-subscribe to destinations if needed
            resubscribeOnConnect()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.v(TAG, "Raw WebSocket message received: $text")
            handleStompFrame(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code / $reason")
            stompConnected = false
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code / $reason")
            stompConnected = false
            // Attempt to reconnect after a delay
            startReconnecting()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${response?.code} / ${response?.message}", t)

            if (t is java.net.SocketException) {
                Log.e(TAG, "Detected SocketException. Resetting WebSocket object.")
            }

            stompConnected = false
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
                stompConnected = true
                Log.i(TAG, "STOMP CONNECTED. Session: ${headers["session"]}")
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
                val message = headers["message"] ?: "Unknown error"
                Log.e(TAG, "STOMP ERROR: $message\nBody:\n$bodyPart")
                // Depending on the error, might need to reconnect
                // e.g., if it's an auth error, might need to refresh token and reconnect
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

            while (isActive && !stompConnected) {
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
            if (!stompConnected) {
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
            val a = 3
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebSocket", e)
            startReconnecting()
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting STOMP client")
        connectionJob?.cancel() // Cancel reconnection job
        stompConnected = false

        val disconnectFrame = "DISCONNECT\n\n\u0000"
        webSocket?.send(disconnectFrame) // Send STOMP DISCONNECT frame

        // Close the underlying WebSocket after a short delay to allow DISCONNECT to send
        applicationScope.launch {
            delay(100) // Small delay
            webSocket?.close(1000, "Client disconnecting")
            webSocket = null // Clear reference
            Log.i(TAG, "WebSocket client disconnected.")
        }
    }

    fun subscribe(destination: String): String {
        if (subscriptions.contains(destination)) {
             Log.w(TAG, "Already subscribed to $destination")
             return subscriptions[destination]!! // Return existing ID
        }

        val subscriptionId = UUID.randomUUID().toString()
        subscriptions[destination] = subscriptionId

        if (stompConnected) {
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
            if (stompConnected) {
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
        if (!stompConnected) {
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

    // Lifecycle method to clean up coroutine scope
    fun shutdown() {
        disconnect() // Ensure disconnect is called
        applicationScope.cancel() // Cancel the scope
        Log.d(TAG, "StompWebSocketClient shut down.")
    }
} 