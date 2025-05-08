package kr.gachon.adigo.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.gachon.adigo.AdigoApplication
import kotlinx.coroutines.launch

class WebSocketTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WebSocketTestScreen()
        }
    }
}

@Composable
fun WebSocketTestScreen() {
    val stompClient = remember { AdigoApplication.stompWebSocketClient }
    val coroutineScope = rememberCoroutineScope()
    var log by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("/user/queue/friendsLocationResponse") }
    var message by remember { mutableStateOf("{\"test\":\"hello\"}") }
    var subscribed by remember { mutableStateOf(false) }

    // Collect incoming messages
    LaunchedEffect(Unit) {
        stompClient.messageFlow.collect { (dest, body) ->
            log += "\n[$dest] $body"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("WebSocket 테스트", style = MaterialTheme.typography.h6)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { stompClient.connect() }) { Text("Connect") }
            Button(onClick = { stompClient.disconnect() }) { Text("Disconnect") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destination") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (!subscribed) {
                    stompClient.subscribe(destination)
                    subscribed = true
                }
            }) { Text("Subscribe") }
            Button(onClick = {
                if (subscribed) {
                    stompClient.unsubscribe(destination)
                    subscribed = false
                }
            }) { Text("Unsubscribe") }
        }
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message Body") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            stompClient.send(destination, message)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Send Message")
        }
        Divider()
        Text("Received Messages:", style = MaterialTheme.typography.subtitle1)
        Text(log, modifier = Modifier.fillMaxWidth())
    }
} 