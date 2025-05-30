package kr.gachon.adigo.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.TelephonyManager
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import kr.gachon.adigo.ui.viewmodel.EmailViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay



// 핸드폰 번호를 가져오는 함수
@SuppressLint("MissingPermission")
fun getPhoneNumber(context: Context): String {
    try {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        // 두 권한 모두 확인
        val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasPhoneNumbersPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d("PhonePermissions", "READ_PHONE_STATE: $hasPhoneStatePermission, READ_PHONE_NUMBERS: $hasPhoneNumbersPermission")
        
        // 두 권한 중 하나라도 있으면 전화번호 가져오기 시도
        if (hasPhoneStatePermission || hasPhoneNumbersPermission) {
            try {
                @Suppress("DEPRECATION")
                val phoneNumber = telephonyManager.line1Number ?: ""
                Log.d("PhoneNumber", "Raw phone number: $phoneNumber")
                if (phoneNumber.isNotEmpty()) {
                    // 숫자만 추출
                    val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
                    return formatPhoneNumber(digitsOnly)
                }
            } catch (e: SecurityException) {
                Log.e("PhoneNumber", "Security exception when getting phone number: ${e.message}")
                e.printStackTrace()
            }
        } else {
            Log.d("PhoneNumber", "No permission to read phone number")
        }
    } catch (e: Exception) {
        Log.e("PhoneNumber", "Error getting phone number: ${e.message}")
        e.printStackTrace()
    }
    return ""
}

// 핸드폰 번호를 포맷팅하는 함수 (01012345678 -> 010-1234-5678)
fun formatPhoneNumber(phoneNumber: String): String {
    if (phoneNumber.isEmpty()) return ""
    
    // 입력값이 이미 숫자만 포함하고 있다고 가정
    return when {
        phoneNumber.length <= 3 -> {
            // 3자리 이하는 그대로 표시 (예: "010")
            phoneNumber
        }
        phoneNumber.length <= 7 -> {
            // 4-7자리는 "010-1234" 형식으로 표시
            phoneNumber.substring(0, 3) + "-" + phoneNumber.substring(3)
        }
        else -> {
            // 8자리 이상은 "010-1234-5678" 형식으로 표시
            val prefix = phoneNumber.substring(0, 3)           // 010
            val middle = phoneNumber.substring(3, 7)           // 5386
            val suffix = phoneNumber.substring(7)              // 3683
            "$prefix-$middle-$suffix"
        }
    }
}

// 진동 효과를 실행하는 함수
fun vibratePhone(context: Context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        // Android 12 이상에서는 VibratorManager 사용
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        // Android 12 미만에서는 기존 Vibrator 사용
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Android 8.0 이상에서는 VibrationEffect 사용
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // Android 8.0 미만에서는 기존 방식 사용
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun EmailInputScreen(
    authViewModel: AuthViewModel,
    emailViewModel: EmailViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var hasAttemptedToLoadPhoneNumber by remember { mutableStateOf(false) }
    
    // 에러 상태 및 흔들림 애니메이션을 위한 상태 변수
    var isLoginError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
//    val email by emailViewModel.email
//    val emailValid by emailViewModel.emailValid
//    val emailDuplicate by emailViewModel.emailDuplicate

    val email          by emailViewModel.email.collectAsStateWithLifecycle()
    val emailValid     by emailViewModel.emailValid.collectAsStateWithLifecycle()
    val emailDuplicate by emailViewModel.emailDuplicate.collectAsStateWithLifecycle()



    // 흔들림 애니메이션 상태
    val shakeState = animateFloatAsState(
        targetValue = if (isLoginError) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        finishedListener = { isLoginError = false }
    )

    // 오류 발생 시 애니메이션 및 진동 효과
    LaunchedEffect(isLoginError) {
        if (isLoginError) {
            vibratePhone(context)
            // 애니메이션이 끝나면 상태 리셋
            delay(500)
            isLoginError = false
        }
    }
    
    // 휴대폰 번호 정규식 (010-1234-5678 형식)
    val phoneRegex = remember { "^01[016789]-\\d{3,4}-\\d{4}\$".toRegex() }
    val isPhoneNumberValid by derivedStateOf { phoneRegex.matches(phoneNumber) }
    
    // 이메일이 유효하고 중복이 아닐 때 전화번호를 가져오기
    LaunchedEffect(emailValid, emailDuplicate) {
        if (emailValid && !emailDuplicate && !hasAttemptedToLoadPhoneNumber) {
            Log.d("EmailInputScreen", "Attempting to load phone number when email is valid and not duplicate")
            hasAttemptedToLoadPhoneNumber = true
            
            val formattedNumber = getPhoneNumber(context)
            Log.d("EmailInputScreen", "Got phone number: $formattedNumber")
            if (formattedNumber.isNotEmpty()) {
                phoneNumber = formattedNumber
            }
        }
    }

    // 흔들림 효과를 적용할 수정자 함수
    fun Modifier.shake(shakeState: Float): Modifier = this.then(
        Modifier.graphicsLayer {
            translationX = if (shakeState > 0f) {
                (shakeState * 10 * kotlin.math.sin(shakeState * 20 * kotlin.math.PI)).toFloat()
            } else {
                0f
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {

        Spacer(Modifier.height(100.dp))

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
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false
            ),
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = if (emailValid) Color(0xFF3F51B5) else Color.Red,
                unfocusedIndicatorColor = if (email.isEmpty()) Color.Gray else if (emailValid) Color(0xFF3F51B5) else Color.Red,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Red,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
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

            // 비밀번호 TextField에 흔들림 효과 적용
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                modifier = Modifier
                    .fillMaxWidth()
                    .shake(shakeState.value),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF3F51B5),
                    unfocusedIndicatorColor = Color.Gray,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                //비밀번호 자동완성 없애기
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(
                    onDone = { /* 키보드 완료 버튼 동작 */ }
                )
            )
            
            // 에러 메시지 표시
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }




            Button(
                onClick = {
                    authViewModel.sendLogin(
                        email, password,
                        onSuccess = {
                            authViewModel.notifyLoginSuccess()
                            navController.navigate(Screens.Main.name){
                                popUpTo(Screens.OnBoard.name){ inclusive = true}
                                launchSingleTop = true
                            }
                        },
                        onError = { errorMsg ->
                            // 로그인 실패 시 에러 상태 활성화
                            Log.d("EmailInputScreen", "Login failed: $errorMsg")
                            errorMessage = if (errorMsg.contains("500")) {
                                "서버 오류가 발생했습니다. 다시 시도해 주세요."
                            } else {
                                "이메일 또는 비밀번호가 올바르지 않습니다."
                            }
                            isLoginError = true
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
                onValueChange = { phoneNumber = it
                },
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
