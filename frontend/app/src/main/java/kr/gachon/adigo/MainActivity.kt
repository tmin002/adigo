package kr.gachon.adigo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.remote.RemoteDataSource
import kr.gachon.adigo.data.remote.httpClient
import kr.gachon.adigo.ui.theme.AdigoTheme
import kr.gachon.adigo.ui.viewmodel.ContentViewModel

class MainActivity : ComponentActivity() {

    // 1) Activity가 소유하는 ViewModel 인스턴스
    private lateinit var viewModel: ContentViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2) 수동 DI: TokenManager 생성
        val tokenManager = TokenManager(this)

        //3) 수동 DI : RemoteDataSource 생성
        val remoteDataSource = httpClient.create(tokenManager)

        // 3) ViewModel 생성 시점에 주입
        viewModel = ContentViewModel(remoteDataSource, tokenManager)

        // 4) setContent에서 Compose UI 호출
        setContent {
            AdigoTheme {
                // LoginMain에 viewModel을 넘겨준다
                LoginMain(viewModel)
            }
        }
    }


    @Composable
    fun LoginMain(viewModel: ContentViewModel) {
        // 예시로 간단한 UI
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                viewModel.sendLogin (email, password,
                    onSuccess = {
                    // 로그인 성공 시 처리
                    // 예: 다음 화면으로 이동, 토큰 저장 여부 확인 등
                },
                    onError = { errorMsg ->

                    })
            }) {
                Text("Login")
            }
        }
    }

    @Preview()
    @Composable
    fun ContentViewPreview() {
        MainActivity()
    }

}
