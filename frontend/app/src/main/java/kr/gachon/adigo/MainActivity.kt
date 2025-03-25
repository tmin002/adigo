package kr.gachon.adigo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import kr.gachon.adigo.ui.theme.AdigoTheme


class MainActivity : ComponentActivity() {

    // 1) Activity가 소유하는 ViewModel 인스턴스
    private lateinit var viewModel: AuthViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2) 수동 DI: TokenManager 생성
        val tokenManager = TokenManager(this)

        //3) 수동 DI : RemoteDataSource 생성
        val remoteDataSource = httpClient.create(tokenManager)

        // 3) ViewModel 생성 시점에 주입
        viewModel = AuthViewModel(remoteDataSource, tokenManager)

        // 4) setContent에서 Compose UI 호출
      develop
        setContent {
            AdigoTheme {
                MainScreen(
                    onSignInClick = {
                        // "시작하기" 버튼 클릭 시 회원가입 액티비티로 이동
                        startActivity(Intent(this, SignUpActivity::class.java))
                    },
                    onLoginClick = {
                        // "로그인" 텍스트 클릭 시 로그인 액티비티로 이동
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                )
            }
        }
    }
}


    @Composable
    fun LoginMain(viewModel: AuthViewModel) {
        // 예시로 간단한 UI
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }


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
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "시작하기")
            }
            Spacer(modifier = Modifier.height(16.dp))
            // "이미 회원이신가요? 로그인" 링크 → 로그인 액티비티로 이동
            Text(
                text = "이미 회원이신가요? 로그인",
                modifier = Modifier.clickable(onClick = onLoginClick)
            )
        }
    }
}
