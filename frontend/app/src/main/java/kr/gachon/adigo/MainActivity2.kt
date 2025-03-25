//package kr.gachon.adigo
//
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.Manifest;
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontFamily
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import kr.gachon.adigo.ui.components.UwbPrecisionLocationPopup
//import kr.gachon.adigo.ui.theme.AdigoTheme
//import kr.gachon.adigo.ui.viewmodel.UwbLocationViewModel
//import kr.gachon.adigo.service.UwbService
//
//class MainActivity : ComponentActivity() {
//
//    companion object {
//        private const val UWB_PERMISSION_REQUEST_CODE = 123
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//
//        val uwbService = UwbService(this)
//        val viewModel = UwbLocationViewModel(uwbService)
//
//
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.UWB_RANGING
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // (2) 권한이 허용 안 되어 있으면, 요청
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.UWB_RANGING),
//                UWB_PERMISSION_REQUEST_CODE
//            )
//        } else {
//
//        }
//
//
//
//        enableEdgeToEdge()
//        setContent {
//            AdigoTheme {
//                LoginView(viewModel)
//            }
//        }
//    }
//}
//
//
//
//
//
//
//@Composable
//fun LoginView(viewModel: UwbLocationViewModel) {
//    var showUwbPopup by remember { mutableStateOf(false) }
//
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        LoginMiddleView()
//        LoginBottomView(
//            onUwbButtonClick = { showUwbPopup = !showUwbPopup }
//        )
//    }
//
//    // 수정된 UwbPrecisionLocationPopup 호출
//    UwbPrecisionLocationPopup(
//        isVisible = showUwbPopup,
//        onDismissRequest = { showUwbPopup = false },
//        viewModel = viewModel
//    )
//}
//
//@Composable
//fun LoginMiddleView() {
//    Column(
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Image(
//            painter = painterResource(id = R.drawable.icon),
//            contentDescription = "Middle Logo",
//            modifier = Modifier.size(300.dp)
//        )
//
//        Text(
//            text = "어디고",
//            fontFamily = FontFamily.Monospace,
//            fontSize = 50.sp
//        )
//    }
//}
//
//@Composable
//fun LoginBottomView(onUwbButtonClick: () -> Unit) {
//    Column {
//        Button(
//            onClick = {  },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(60.dp)
//                .padding(start = 20.dp, end = 20.dp),
//            elevation = ButtonDefaults.buttonElevation(2.dp),
//            shape = RoundedCornerShape(150.dp),
//            colors = ButtonDefaults.buttonColors(Color.Black)
//        ) {
//            Text(text = "로그인")
//        }
//
//        Button(
//            onClick = { /* TODO: 회원가입 */ },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(70.dp)
//                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
//            elevation = ButtonDefaults.buttonElevation(2.dp),
//            shape = RoundedCornerShape(150.dp),
//            colors = ButtonDefaults.buttonColors(Color.Black)
//        ) {
//            Text(text = "회원가입")
//        }
//
//        // 정밀 위치 탐색 버튼 (onUwbButtonClick 호출)
//        Button(
//            onClick = onUwbButtonClick,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(70.dp)
//                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
//            elevation = ButtonDefaults.buttonElevation(2.dp),
//            shape = RoundedCornerShape(150.dp),
//            colors = ButtonDefaults.buttonColors(Color.Black)
//        ) {
//            Text(text = "정밀위치탐색")
//        }
//    }
//}