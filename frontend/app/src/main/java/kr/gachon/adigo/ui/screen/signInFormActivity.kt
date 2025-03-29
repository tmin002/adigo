package kr.gachon.adigo.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import kr.gachon.adigo.ui.viewmodel.EmailViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class signInForm {

}


@SuppressLint("UnrememberedMutableState")
@Composable
fun EmailInputScreen(
    authViewModel: AuthViewModel,
    emailViewModel: EmailViewModel,
    navController: NavController
) {

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
        Text(
            if (emailDuplicate) "이미 회원이시군요!" else "본인 확인을 위해\n이메일을 입력해주세요",
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
                unfocusedIndicatorColor = if (email.isEmpty()) Color.Gray else if (emailValid && !emailDuplicate) Color(
                    0xFF3F51B5
                ) else Color.Red,
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
                            navController.navigate(Screens.Main.name)
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
                        val encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
                        var encodedPhoneNumber = URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screens.VerifyCode.name + "/$encodedEmail" + "/$encodedPhoneNumber")

                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("다음")
                }
            }
        }
    }


}
