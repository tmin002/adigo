package kr.gachon.adigo.ui.screen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.gachon.adigo.MainActivity
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.remote.httpClient
import kr.gachon.adigo.ui.theme.AdigoTheme
import kr.gachon.adigo.ui.viewmodel.AuthViewModel

class LoginActivity : ComponentActivity() {

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
        setContent {
            AdigoTheme {
                // LoginMain에 viewModel을 넘겨준다
                LoginMain(viewModel,this)
            }
        }
    }


    @Composable
    fun LoginMain(viewModel: AuthViewModel,activity: LoginActivity) {
        // 예시로 간단한 UI
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.Companion.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.Companion.fillMaxWidth().padding(4.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                modifier = Modifier.Companion.fillMaxWidth().padding(4.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.Companion.height(16.dp))
            Button(onClick = {
//                viewModel.sendLogin(
//                    email, password,
//                    onSuccess = {
//
//                    },
//                    onError = { errorMsg ->
//
//                    })
                val intent = Intent(activity, MainScreenActivity::class.java)
                activity.startActivity(intent)


            }) {
                Text("Login")
            }
        }
    }

    @Preview()
    @Composable
    fun ContentViewPreview() {
        LoginActivity()
    }

}