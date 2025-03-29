package kr.gachon.adigo

import VerificationCodeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kr.gachon.adigo.data.remote.httpClient
import kr.gachon.adigo.ui.screen.EmailInputScreen
import kr.gachon.adigo.ui.screen.PersistentBottomSheetMapScreen
import kr.gachon.adigo.ui.screen.Screens
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
                Main(viewModel, this)
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
    fun Main(viewModel: AuthViewModel, activity: MainActivity) {


        val navController = rememberNavController()


        NavHost(
            navController = navController,
            startDestination = Screens.OnBoard.name,
        ) {
            composable(route = Screens.OnBoard.name) {
                onBoard(navController)
            }
            composable(route = Screens.SignIn.name) {


                EmailInputScreen(
                    viewModel,
                    EmailViewModel(AdigoApplication.httpService),
                    navController
                )


            }
            composable(route = Screens.VerifyCode.name) {
                VerificationCodeScreen(
                    onBackPress = {navController.popBackStack()},
                    onResendClick = {

                    }
                ) {

                }
            }
            composable(route = Screens.Main.name) {
                PersistentBottomSheetMapScreen()
            }


        }


    }


}
