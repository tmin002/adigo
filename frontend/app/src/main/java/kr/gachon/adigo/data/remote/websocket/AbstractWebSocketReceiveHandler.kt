package kr.gachon.adigo.data.remote.websocket

interface AbstractWebSocketReceiveHandler {
   val receiveActionTable: Map<String, (String) -> Unit>
}