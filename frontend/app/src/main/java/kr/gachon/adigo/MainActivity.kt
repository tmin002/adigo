package kr.gachon.adigo

import VerificationCodeScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
                        tokenManager = container.tokenManager,
                        authVm = authVm,
                        uwbVm  = uwbVm
                    )
                }
            }
        }
    }

    @Composable
    fun MainNavHost(
        tokenManager: TokenManager,
        authVm: AuthViewModel,
        uwbVm: UwbLocationViewModel
    ) {
        val navController = rememberNavController()

        /* ── 시작 목적지 결정 ── */
        val startDest = remember {
            if (!tokenManager.isTokenExpired() && tokenManager.getJwtToken() != null)
                Screens.Main.name
            else
                Screens.OnBoard.name
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




//class MainActivity : ComponentActivity() {
//
//    // 1) Activity가 소유하는 ViewModel 인스턴스
//    private lateinit var viewModel: AuthViewModel
//    private lateinit var uwbviewModel : UwbLocationViewModel
//    private lateinit var friendLocationViewModel: FriendLocationViewModel
//
//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (!isGranted) {
//                Toast.makeText(this, "UWB 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        // 2) 수동 DI: TokenManager 생성
//        val tokenManager = AdigoApplication.AppContainer.tokenManager
//
//        var uwbService = uwbService(this)
//
//        // 3) ViewModel 생성 시점에 주입
//        viewModel = AuthViewModel()
//
//        uwbviewModel = UwbLocationViewModel(uwbService)
//
//
//
//
//        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//            if (!task. isSuccessful) {
//                Log.d("push", "Fetching FCM registration token failed", task.exception)
//                return@OnCompleteListener
//            }
//
//            val token = task. result
//            tokenManager.saveDeviceToken(token)
//            Log.d("FCMTOKEN", token)
//        })
//
//
//
//
//        // 4) setContent에서 Compose UI 호출
//        setContent {
//            AdigoTheme {
//                Main(viewModel,tokenManager,this)
//            }
//        }
//
//        // 앱 실행 시 UWB 권한 요청
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ContextCompat.checkSelfPermission(this, Manifest.permission.UWB_RANGING)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestPermissionLauncher.launch(Manifest.permission.UWB_RANGING)
//        }
//
//
//    }
//
//
//
//
//    @Composable
//    fun onBoard(navController: NavController) {
//
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center,
//            modifier = Modifier.padding(16.dp)
//        ) {
//            // 앱 로고 (리소스 ID는 실제 앱 로고에 맞게 수정)
//            Image(
//                painter = painterResource(id = R.drawable.logo1),
//                contentDescription = "앱 로고",
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            // 간단한 텍스트 레이블
//            Text(text = "니 지금 어디고?")
//            Text(text = "친구가 어디 있는지 모르는 답답한 상황엔 '어디고'")
//            Spacer(modifier = Modifier.height(24.dp))
//            // "시작하기" 버튼 → 회원가입 액티비티로 이동
//            Button(
//                onClick = {
//                    navController.navigate(Screens.SignIn.name)
//                },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text(text = "시작하기")
//            }
//            UwbPrecisionLocationPopup(isVisible = true, onDismissRequest = {}, viewModel = uwbviewModel)
//            Spacer(modifier = Modifier.height(16.dp))
//
//        }
//
//    }
//
//
//    @Composable
//    fun Main(viewModel: AuthViewModel,tokenManager: TokenManager, activity: MainActivity) {
//        val navController = rememberNavController()
//
//        // 권한 요청을 위한 launcher
//        val permissionLauncher = rememberLauncherForActivityResult(
//            ActivityResultContracts.RequestPermission()
//        ) { isGranted: Boolean ->
//            if (isGranted) {
//                Log.d("MainActivity", "READ_PHONE_STATE permission granted")
//            } else {
//                Log.d("MainActivity", "READ_PHONE_STATE permission denied")
//            }
//        }
//
//        // <<< 시작 화면 동적 결정 >>>
//        //jwt토큰이 존재하고 동시에 토큰이 만료되지 않았다면 메인창으로 이동
//        val startDestination = remember {
//            if (tokenManager.isTokenExpired()==false && tokenManager.getJwtToken()!=null) {
//                Log.d("MainActivity", "Valid token found. Starting with Main screen.")
//                Screens.Main.name
//            } else {
//                Log.d("MainActivity", "No valid token. Starting with OnBoard screen.")
//                Screens.OnBoard.name
//            }
//        }
//
//        // 앱 시작 시 전화번호 읽기 권한 요청
//        LaunchedEffect(Unit) {
//            val phoneStatePermission = ContextCompat.checkSelfPermission(
//                activity,
//                Manifest.permission.READ_PHONE_STATE
//            ) == PackageManager.PERMISSION_GRANTED
//
//            val phoneNumbersPermission = ContextCompat.checkSelfPermission(
//                activity,
//                Manifest.permission.READ_PHONE_NUMBERS
//            ) == PackageManager.PERMISSION_GRANTED
//
//            // 둘 다 권한이 없으면 먼저 READ_PHONE_STATE 요청
//            if (!phoneStatePermission) {
//                permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
//            }
//            // READ_PHONE_STATE 권한은 있지만 READ_PHONE_NUMBERS 권한이 없으면 요청
//            else if (!phoneNumbersPermission) {
//                permissionLauncher.launch(Manifest.permission.READ_PHONE_NUMBERS)
//            }
//        }
//
//
//        NavHost(
//            navController = navController,
//            startDestination = startDestination,
//        ) {
//            composable(route = Screens.OnBoard.name) {
//                onBoard(navController)
//            }
//            composable(
//                route = Screens.SignIn.name
//            ) {
//                EmailInputScreen(
//                    viewModel,
//                    EmailViewModel(),
//                    navController
//                )
//            }
//            composable(
//                route = Screens.VerifyCode.name + "/{Email}"+"/{phonenumber}",
//                arguments = listOf(
//                    navArgument("Email") { type = NavType.StringType },
//                    navArgument("phonenumber") { type = NavType.StringType }
//                )
//            ) { backStackEntry ->
//                val email = backStackEntry.arguments?.getString("Email") ?: ""
//                val phonenumber = backStackEntry.arguments?.getString("phonenumber") ?: ""
//                VerificationCodeScreen(
//                    viewModel, email, phonenumber, navController,
//                    onBackPress =
//                        {
//                            navController.popBackStack()
//                        },
//                )
//            }
//            composable(
//                route = Screens.FinalSignUp.name + "/{Email}/{phonenumber}",
//                arguments = listOf(
//                    navArgument("Email") { type = NavType.StringType },
//                    navArgument("phonenumber") { type = NavType.StringType }
//                )
//            ){ backStackEntry ->
//                val email = backStackEntry.arguments?.getString("Email") ?: ""
//                val phonenumber = backStackEntry.arguments?.getString("phonenumber") ?: ""
//                FinalSignUpScreen(viewModel, email, phonenumber, navController)
//            }
//            composable(route = Screens.Main.name) {
//                MapScreen(viewModel,navController)
//            }
//            // Register the websocket_test route
//            addWebSocketTestRoute(navController)
//        }
//
//
//    }
//
//
//}
