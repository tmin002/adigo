package kr.gachon.adigo.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.gachon.adigo.ui.theme.AdigoTheme
import kr.gachon.adigo.ui.viewmodel.EmailViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kr.gachon.adigo.AdigoApplication.Companion.tokenManager
import kr.gachon.adigo.data.remote.httpClient


class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //3) 수동 DI : RemoteDataSource 생성
        val remoteDataSource = httpClient.create(tokenManager)

        // 3) ViewModel 생성 시점에 주입
        val emailviewModel = EmailViewModel(remoteDataSource)


        setContent {
            AdigoTheme {
                EmailInputScreen(emailViewModel = emailviewModel)
            }
        }
    }

    @Composable
    fun EmailInputScreen(emailViewModel: EmailViewModel) {
        val email by emailViewModel.email
        val emailValid by emailViewModel.emailValid
        val emailDuplicate by emailViewModel.emailDuplicate

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "본인 확인을 위해\n이메일을 입력해주세요",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "가입하는 본인의 정보를 작성해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(32.dp))

            TextField(
                value = email,
                onValueChange = { emailViewModel.onEmailChanged(it) },
                label = { Text("이메일") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = if (emailValid && !emailDuplicate) Color(0xFF3F51B5) else Color.Red,
                    unfocusedIndicatorColor = if (email.isEmpty()) Color.Gray else if (emailValid && !emailDuplicate) Color(0xFF3F51B5) else Color.Red,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Red
                )
            )


            if (!emailValid && email.isNotEmpty()) {
                Text(
                    text = "이메일 형식이 올바르지 않습니다.",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (emailDuplicate) {
                Text(
                    text = "이미 가입된 이메일입니다.",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (emailValid && !emailDuplicate) {
                Text(
                    text = "사용 가능한 이메일입니다.",
                    color = Color(0xFF3F51B5),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }



}
}