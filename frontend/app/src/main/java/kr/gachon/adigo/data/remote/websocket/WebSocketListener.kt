package kr.gachon.adigo.data.remote.websocket

import kr.gachon.adigo.AdigoApplication
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class StompWebSocketListener: WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        val connectFrame = buildString {
            append("CONNECT\n")
            append("accept-version:1.1,1.2\n")
            append("heart-beat:0,0\n")
            append("Authorization:Bearer ${AdigoApplication.Companion.tokenManager.getJwtToken()}\n")
            append("\n\u0000")
        }
        webSocket.send(connectFrame)
    }
    override fun onMessage(webSocket: WebSocket, text: String) {
        /*
        TODO:
            1. 메시지 파싱 -> 등록된 AbstractWebSocketDTO로 변환
            2. 변환 성공한 AbstractWetSocketDTO에 해당하는 AbstractWebSocketRepository에서 해당 데이터 처리
            백엔드에서 클라이언트로 보내주는 데이터에 대한 구체적인 구현이 완료되면 작업 예정
         */
        println("STOMP rcv: ${text}")
    }
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("STOMP fail: ${t.message}")
    }
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        println("STOMP terminate: $reason")
    }
}