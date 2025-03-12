package kr.gachon.adigo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.uwb.UwbAddress
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.gachon.adigo.ui.components.UwbPrecisionLocationPopup
import kr.gachon.adigo.ui.theme.AdigoTheme
import kr.gachon.adigo.ui.viewmodel.UwbLocationViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdigoTheme {
                LoginView()
            }
        }
    }
}






@Composable
fun LoginView() {
    var showUwbPopup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoginMiddleView()
        LoginBottomView(
            onUwbButtonClick = { showUwbPopup = true }
        )
    }

    if (showUwbPopup) {
        UwbPrecisionLocationPopup(
            onDismissRequest = { showUwbPopup = false }
        )
    }
}

@Composable
fun LoginMiddleView() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = "Middle Logo",
            modifier = Modifier.size(300.dp)
        )

        Text(
            text = "어디고",
            fontFamily = FontFamily.Monospace,
            fontSize = 50.sp
        )
    }
}

@Composable
fun LoginBottomView(onUwbButtonClick: () -> Unit) {
    Button(
        onClick = { /*TODO: 로그인*/ },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(start = 20.dp, end = 20.dp),
        elevation = ButtonDefaults.buttonElevation(2.dp),
        shape = RoundedCornerShape(150.dp),
        colors = ButtonDefaults.buttonColors(Color.Black)
    ) {
        Text(text = "로그인")
    }

    Button(
        onClick = { /* TODO: 회원가입 */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        elevation = ButtonDefaults.buttonElevation(2.dp),
        shape = RoundedCornerShape(150.dp),
        colors = ButtonDefaults.buttonColors(Color.Black)
    ) {
        Text(text = "회원가입")
    }

    // 새로 추가한 "정밀위치탐색" 버튼
    Button(
        onClick = onUwbButtonClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        elevation = ButtonDefaults.buttonElevation(2.dp),
        shape = RoundedCornerShape(150.dp),
        colors = ButtonDefaults.buttonColors(Color.Black)
    ) {
        Text(text = "정밀위치탐색")
    }
}
