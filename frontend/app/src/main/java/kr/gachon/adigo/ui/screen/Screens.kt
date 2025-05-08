package kr.gachon.adigo.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

enum class Screens {
    OnBoard,
    SignIn,
    Main,
    VerifyCode,
    FinalSignUp,
}

fun NavGraphBuilder.addWebSocketTestRoute(navController: NavController) {
    composable("websocket_test") {
        // Launch the activity and pop back stack
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(Intent(context, WebSocketTestActivity::class.java))
            navController.popBackStack()
        }
        // Optionally, show a loading or transition screen
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("WebSocket 테스트 화면으로 이동 중...")
        }
    }
}


