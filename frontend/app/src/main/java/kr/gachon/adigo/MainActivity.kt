package kr.gachon.adigo

import VerificationCodeScreen
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kr.gachon.adigo.background.UserLocationProviderService
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.service.uwbService
import kr.gachon.adigo.ui.components.PermissionGate
import kr.gachon.adigo.ui.components.UwbPrecisionLocationPopup
import kr.gachon.adigo.ui.screen.EmailInputScreen
import kr.gachon.adigo.ui.screen.FinalSignUpScreen
import kr.gachon.adigo.ui.screen.Screens
import kr.gachon.adigo.ui.screen.addWebSocketTestRoute
import kr.gachon.adigo.ui.screen.map.MapScreen
import kr.gachon.adigo.ui.theme.AdigoTheme
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import kr.gachon.adigo.ui.viewmodel.EmailViewModel
import kr.gachon.adigo.ui.viewmodel.FriendLocationViewModel
import kr.gachon.adigo.ui.viewmodel.UwbLocationViewModel



class MainActivity : ComponentActivity() {

    private lateinit var authVm: AuthViewModel
    private lateinit var uwbVm : UwbLocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "init getToken() = $token")
                AdigoApplication.AppContainer.tokenManager.saveDeviceToken(token)
            } else {
                Log.e("FCM", "token fetch failed", task.exception)
            }
        }

        val container = AdigoApplication.AppContainer
        authVm = AuthViewModel()
        uwbVm  = UwbLocationViewModel(uwbService(this))

        setContent {
            AdigoTheme {
                PermissionGate {
                    MainNavHost(
                        authVm = authVm,
                        uwbVm  = uwbVm
                    )
                }
            }
        }
    }

    @Composable
    fun MainNavHost(
        authVm: AuthViewModel,
        uwbVm: UwbLocationViewModel
    ) {
        val isLoggedIn by authVm.isLoggedIn.collectAsState()
        val navController = rememberNavController()
        val startDest = if (isLoggedIn) Screens.Main.name else Screens.OnBoard.name


        LaunchedEffect(isLoggedIn) {
            val svcIntent = Intent(this@MainActivity, UserLocationProviderService::class.java)
            if (isLoggedIn) {
                // 로그인 → 포그라운드 서비스 시작
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(svcIntent)
                else
                    startService(svcIntent)
            } else {
                // 로그아웃 → 서비스 중지
                stopService(svcIntent)
                AdigoApplication.AppContainer.stompClient.disconnect()
            }

            // 로그인 상태가 바뀔 때마다 내비게이션 스택도 초기화
            navController.navigate(startDest) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }



        NavHost(
            navController = navController,
            startDestination = startDest
        ) {

            /* ────────────── 온보딩 ────────────── */
            composable(route = Screens.OnBoard.name) {
                OnBoardScreen(navController = navController, uwbVm = uwbVm)
            }

            /* ────────────── 이메일 입력 / 로그인 ────────────── */
            composable(route = Screens.SignIn.name) {
                EmailInputScreen(
                    authVm,
                    EmailViewModel(),
                    navController
                )
            }

            /* ────────────── 이메일 인증 코드 입력 ────────────── */
            composable(
                route = Screens.VerifyCode.name + "/{Email}/{phonenumber}",
                arguments = listOf(
                    navArgument("Email")        { type = NavType.StringType },
                    navArgument("phonenumber")  { type = NavType.StringType }
                )
            ) { backStack ->
                val email = backStack.arguments?.getString("Email") ?: ""
                val phone = backStack.arguments?.getString("phonenumber") ?: ""
                VerificationCodeScreen(
                    authVm,
                    email,
                    phone,
                    navController,
                    onBackPress = { navController.popBackStack() }
                )
            }

            /* ────────────── 최종 회원가입 ────────────── */
            composable(
                route = Screens.FinalSignUp.name + "/{Email}/{phonenumber}",
                arguments = listOf(
                    navArgument("Email")       { type = NavType.StringType },
                    navArgument("phonenumber") { type = NavType.StringType }
                )
            ) { backStack ->
                val email = backStack.arguments?.getString("Email") ?: ""
                val phone = backStack.arguments?.getString("phonenumber") ?: ""
                FinalSignUpScreen(
                    authVm,
                    email,
                    phone,
                    navController
                )
            }

            /* ────────────── 메인 지도 화면 ────────────── */
            composable(route = Screens.Main.name) {
                MapScreen(
                    authViewModel = authVm,
                    navController = navController
                )
            }

            /* ────────────── 테스트용 웹소켓 화면 ────────────── */
            addWebSocketTestRoute(navController)
        }
    }


    @Composable
    fun OnBoardScreen(navController: NavController, uwbVm: UwbLocationViewModel) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // 앱 로고 (리소스 ID는 실제 앱 로고에 맞게 수정)
            Image(
                painter = painterResource(id = R.drawable.logo1),
                contentDescription = "앱 로고",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // 간단한 텍스트 레이블
            Text(text = "니 지금 어디고?")
            Text(text = "친구가 어디 있는지 모르는 답답한 상황엔 '어디고'")
            Spacer(modifier = Modifier.height(24.dp))
            // "시작하기" 버튼 → 회원가입 액티비티로 이동
            Button(
                onClick = {
                    navController.navigate(Screens.SignIn.name)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "시작하기")
            }
            UwbPrecisionLocationPopup(isVisible = true, onDismissRequest = {}, viewModel = uwbVm)
            Spacer(modifier = Modifier.height(16.dp))

        }

    }


}




