package kr.gachon.adigo.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kr.gachon.adigo.AdigoApplication.Companion.tokenManager
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.remote.httpClient
import kr.gachon.adigo.ui.viewmodel.AuthViewModel


class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // 2) 수동 DI: TokenManager 생성
        val tokenManager = TokenManager(this)


        //3) 수동 DI : RemoteDataSource 생성
        val remoteDataSource = httpClient.create(tokenManager)

        // 3) ViewModel 생성 시점에 주입
        val emailviewModel = EmailViewModel(remoteDataSource)

        // 3) ViewModel 생성 시점에 주입
        var authViewModel = AuthViewModel(remoteDataSource, tokenManager)


        setContent {
            AdigoTheme {
                EmailInputScreen(authViewModel,emailViewModel = emailviewModel,this)
            }
        }
    }


    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun EmailInputScreen(authViewModel: AuthViewModel, emailViewModel: EmailViewModel,activity: SignUpActivity) {

        var password by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }


        val email by emailViewModel.email
        val emailValid by emailViewModel.emailValid
        val emailDuplicate by emailViewModel.emailDuplicate

        // 휴대폰 번호 정규식 (010-1234-5678 형식)
        val phoneRegex = remember { "^01[016789]-\\d{3,4}-\\d{4}\$".toRegex() }
        val isPhoneNumberValid by derivedStateOf { phoneRegex.matches(phoneNumber) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text( if (emailDuplicate) "이미 회원이시군요!" else "본인 확인을 위해\n이메일을 입력해주세요",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (emailDuplicate) "다시 만나서 반가워요!" else "가입하는 본인의 정보를 작성해주세요.",
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
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
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
                    text = "",
                    color = Color.Blue,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("비밀번호") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF3F51B5),
                        unfocusedIndicatorColor = Color.Gray
                    )
                )

                Button(
                    onClick = {

                        authViewModel.sendLogin(
                            email, password,
                            onSuccess = {
                                val intent = Intent(activity, MainScreenActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                activity.startActivity(intent)
                            },
                            onError = { errorMsg ->

                            })



                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("로그인")
                }



            } else if (emailValid && !emailDuplicate) {
                Text(
                    text = "사용 가능한 이메일입니다.",
                    color = Color.Blue,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )

                TextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("휴대전화번호 (010-1234-5678)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF3F51B5),
                        unfocusedIndicatorColor = Color.Gray
                    )
                )

                if (isPhoneNumberValid) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {

                        /* 다음 단계로 이동하는 로직 추가 */
                        
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("다음")
                    }
                }
            }
        }



}

}