package kr.gachon.adigo

import VerificationCodeScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.remote.httpClient

import kr.gachon.adigo.ui.screen.EmailInputScreen
import kr.gachon.adigo.ui.screen.FinalSignUpScreen
import kr.gachon.adigo.ui.screen.Screens
import kr.gachon.adigo.ui.screen.getPhoneNumber
import kr.gachon.adigo.ui.screen.map.MapScreen
import kr.gachon.adigo.ui.theme.AdigoTheme
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import kr.gachon.adigo.ui.viewmodel.EmailViewModel


class MainActivity : ComponentActivity() {

    // 1) Activity가 소유하는 ViewModel 인스턴스
    private lateinit var viewModel: AuthViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2) 수동 DI: TokenManager 생성
        val tokenManager = AdigoApplication.tokenManager

        //3) 수동 DI : RemoteDataSource 생성
        val remoteDataSource = httpClient.create(tokenManager)

        // 3) ViewModel 생성 시점에 주입
        viewModel = AuthViewModel(remoteDataSource, tokenManager)




        // 4) setContent에서 Compose UI 호출
        setContent {
            AdigoTheme {
                Main(viewModel,tokenManager,this)
            }
        }


    }




    @Composable
    fun onBoard(navController: NavController) {

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
            Spacer(modifier = Modifier.height(16.dp))

        }

    }


    @Composable
    fun Main(viewModel: AuthViewModel,tokenManager: TokenManager, activity: MainActivity) {
        val navController = rememberNavController()

        // 권한 요청을 위한 launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "READ_PHONE_STATE permission granted")
            } else {
                Log.d("MainActivity", "READ_PHONE_STATE permission denied")
            }
        }

        // <<< 시작 화면 동적 결정 >>>
        //jwt토큰이 존재하고 동시에 토큰이 만료되지 않았다면 메인창으로 이동
        val startDestination = remember {
            if (tokenManager.isTokenExpired()==false && tokenManager.getJwtToken()!=null) {
                Log.d("MainActivity", "Valid token found. Starting with Main screen.")
                Screens.Main.name
            } else {
                Log.d("MainActivity", "No valid token. Starting with OnBoard screen.")
                Screens.OnBoard.name
            }
        }

        // 앱 시작 시 전화번호 읽기 권한 요청
        LaunchedEffect(Unit) {
            val phoneStatePermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
            
            val phoneNumbersPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED

            // 둘 다 권한이 없으면 먼저 READ_PHONE_STATE 요청
            if (!phoneStatePermission) {
                permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            } 
            // READ_PHONE_STATE 권한은 있지만 READ_PHONE_NUMBERS 권한이 없으면 요청
            else if (!phoneNumbersPermission) {
                permissionLauncher.launch(Manifest.permission.READ_PHONE_NUMBERS)
            }
        }


        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(route = Screens.OnBoard.name) {
                onBoard(navController)
            }
            composable(
                route = Screens.SignIn.name
            ) {


                EmailInputScreen(
                    viewModel,
                    EmailViewModel(AdigoApplication.httpService),
                    navController
                )


            }
            composable(
                route = Screens.VerifyCode.name + "/{Email}"+"/{phonenumber}",
                arguments = listOf(
                    navArgument("Email") { type = NavType.StringType },
                    navArgument("phonenumber") { type = NavType.StringType } // <<< phonenumber 인자 정의 추가
                )
            ) { backStackEntry ->
                // 3. 인자 가져오기: 정의된 키("Email", "phonenumber")를 사용하여 두 인자 모두 가져옴
                val email = backStackEntry.arguments?.getString("Email") ?: ""
                val phonenumber = backStackEntry.arguments?.getString("phonenumber") ?: "" // <<< phonenumber 인자 가져오기 추가


                VerificationCodeScreen(
                    viewModel, email, phonenumber, navController,
                    onBackPress =
                        {
                            navController.popBackStack()
                        },
                )

            }
            composable(
                // route 패턴 수정: '+' 를 '/' 로 변경
                route = Screens.FinalSignUp.name + "/{Email}/{phonenumber}",
                arguments = listOf(
                    navArgument("Email") { type = NavType.StringType },
                    navArgument("phonenumber") { type = NavType.StringType } // 인자 정의는 그대로 유지
                )
            ){ backStackEntry ->
                // 인자 가져오기
                val email = backStackEntry.arguments?.getString("Email") ?: ""
                val phonenumber = backStackEntry.arguments?.getString("phonenumber") ?: ""

                // FinalSignUpScreen 호출 (이전 코드에서처럼 ViewModel, 인자, NavController 전달)
                FinalSignUpScreen(viewModel, email, phonenumber, navController)
            }
            composable(route = Screens.Main.name) {
                MapScreen()
            }


        }


    }


}
