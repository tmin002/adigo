import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock // Changed from Timer in the previous user code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight // Import FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kr.gachon.adigo.ui.screen.Screens
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- Composable for the Verification Code Input Field ---
// (VerificationCodeInputField and VerificationCodeDigitInput composables remain the same as before)
@Composable
fun VerificationCodeInputField(
    modifier: Modifier = Modifier,
    codeLength: Int, // Removed default value here to enforce setting it
    onCodeComplete: (String) -> Unit
) {
    // ... (Implementation is identical to the previous version)
    // 1. State to hold the input value for each digit field
    val codeDigits = remember { List(codeLength) { mutableStateOf("") } }

    // 2. State to hold the complete code string (derived from individual digits)
    val completeCode by remember {
        derivedStateOf { codeDigits.joinToString("") { it.value } }
    }

    // 3. Focus Requesters to manage focus between fields
    val focusRequesters = remember { List(codeLength) { FocusRequester() } }

    // Automatically request focus on the first field when the composable appears
    LaunchedEffect(Unit) {
        delay(100) // Small delay to ensure UI is ready
        // Check if focusRequesters is not empty before accessing index 0
        if (focusRequesters.isNotEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }

    // Notify the caller when the code is complete
    LaunchedEffect(completeCode) {
        if (completeCode.length == codeLength) {
            onCodeComplete(completeCode)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween // Or Arrangement.spacedBy(8.dp)
    ) {
        repeat(codeLength) { index ->
            VerificationCodeDigitInput( // Renamed composable
                digitValue = codeDigits[index].value,
                focusRequester = focusRequesters[index],
                onValueChange = { newValue ->
                    // Allow only single digit or empty string
                    val filteredValue = newValue.filter { it.isDigit() }.take(1)

                    // Update the current digit's state
                    codeDigits[index].value = filteredValue

                    if (filteredValue.isNotEmpty()) {
                        // Move focus to the next field if available
                        if (index < codeLength - 1) {
                            focusRequesters[index + 1].requestFocus()
                        } else {
                            // Optional: Hide keyboard if it's the last digit
                        }
                    } else { // Handle backspace on empty field
                        // Move focus to the previous field if available
                        if (index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun VerificationCodeDigitInput( // Renamed composable
    digitValue: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit
) {
    // ... (Implementation is identical to the previous version)
    Box(
        modifier = Modifier
            .width(45.dp) // Adjust width as needed
            .height(50.dp), // Adjust height as needed
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = digitValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = Color.Black // 다크 모드에서도 검정색으로 표시
            ),
            decorationBox = { innerTextField ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.weight(1f), // Pushes the divider down
                        contentAlignment = Alignment.Center
                    ) {
                        innerTextField() // Display the actual text field content (the digit)
                    }
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = if (digitValue.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray // Change color based on input
                    )
                }
            }
        )
    }
}


// --- VerificationCodeScreen 수정 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationCodeScreen(
    viewModel: AuthViewModel,
    Email: String, // 이전 화면에서 전달받은 값
    phonenumber: String,
    navcontroller: NavController,
    onBackPress: () -> Unit,
    // onSubmitCode 파라미터 제거됨
) {
    // --- State ---
    val codeLength = 4
    var enteredCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) } // 로딩 상태 추가
    var errorMessage by remember { mutableStateOf<String?>(null) } // 에러 메시지 상태 추가

    // Timer state
    var timeLeft by remember { mutableStateOf(5 * 60) }
    val timerText = remember(timeLeft) { "%02d:%02d".format(timeLeft / 60, timeLeft % 60) }
    LaunchedEffect(key1 = timeLeft) { if (timeLeft > 0 && !isLoading) { delay(1000L); timeLeft-- } } // 로딩 중 아닐 때만 타이머 감소

    // 인증번호 발송 요청 (화면 시작 시)
    LaunchedEffect(Unit) {
        Log.d("VerificationCodeScreen", "Requesting verification code for: $phonenumber")
        viewModel.sendVerificationCode(phonenumber,
            onSuccess = {
                Log.i("VerificationCodeScreen", "Verification code sent successfully request initiated.")

            },
            onError = { errorMsg ->
                Log.e("VerificationCodeScreen", "Failed to send verification code: $errorMsg")
                errorMessage = "인증번호 발송 실패: $errorMsg" // 화면 내 에러 표시
                // Toast.makeText(context, "인증번호 발송 실패: $errorMsg", Toast.LENGTH_LONG).show()
                // 필요시 onBackPress() 호출 고려
            }
        )
    }


    fun onVerificationSuccess() {
        val encodedEmail = URLEncoder.encode(Email, StandardCharsets.UTF_8.toString())
        var encodedPhoneNumber = URLEncoder.encode(phonenumber, StandardCharsets.UTF_8.toString())
        navcontroller.navigate(Screens.FinalSignUp.name + "/$encodedEmail" + "/$encodedPhoneNumber")

    }

    // 코드 검증 로직 함수화 (중복 제거)
    val verifyCodeAction: (String) -> Unit = verifyCodeAction@{ codeToVerify ->
        if (isLoading) return@verifyCodeAction // 람다 종료

        isLoading = true
        errorMessage = null
        // 파라미터 이름을 phoneNumberOrEmail로 사용하는 것이 더 명확해 보입니다.
        Log.d("VerificationCodeScreen", "Verifying code: $codeToVerify for ${phonenumber}l")
        viewModel.verifyCode(
            phonenumber, // ViewModel 함수 파라미터 이름에 맞게 전달
            code = codeToVerify,
            onSuccess = {
                isLoading = false
                Log.i("VerificationCodeScreen", "Code verification successful!")
                onVerificationSuccess()
            },
            onError = { errorMsg ->
                isLoading = false
                Log.e("VerificationCodeScreen", "Code verification failed: $errorMsg")
                errorMessage = "인증 실패: $errorMsg"
            }
        )
    }


    BackHandler { onBackPress() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title */ },
                navigationIcon = {
                    IconButton(onClick = onBackPress, enabled = !isLoading) { // 로딩 중 아닐 때만 활성화
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로 가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "보내드린 인증번호를\n입력해주세요",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )

            // Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = "남은 시간", tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(timerText, color = Color.Gray, fontSize = 14.sp)
            }

            // --- Verification Code Input Field ---
            VerificationCodeInputField(
                codeLength = codeLength,
                onCodeComplete = { code ->
                    println("인증 코드 입력됨: $code")
                    enteredCode = code
                    // 코드가 자동으로 완성되면 검증 실행
                    verifyCodeAction(code) // <<< 내부 함수 호출
                },
                modifier = Modifier.padding(vertical = 16.dp)
                // 여기에 isEnabled = !isLoading 추가 가능 (입력 필드 비활성화)
            )

            // 에러 메시지 표시
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Resend Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    enabled = !isLoading, // 로딩 중 아닐 때만 활성화
                    onClick = {
                        if (isLoading) return@clickable
                        Log.d("VerificationCodeScreen", "Resend clicked for: $phonenumber")
                        errorMessage = null // 이전 에러 메시지 초기화
                        timeLeft = 5 * 60 // 타이머 리셋
                        viewModel.sendVerificationCode(
                            phonenumber,
                            onSuccess = {
                                Log.i("VerificationCodeScreen", "Resent verification code successfully.")
                            },
                            onError = { errorMsg ->
                                Log.e("VerificationCodeScreen", "Failed to resend verification code: $errorMsg")
                                errorMessage = "재발송 실패: $errorMsg" // 화면 내 에러 표시
                            }
                        )
                    }
                ).padding(vertical = 4.dp) // 클릭 영역 확보
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (isLoading) Color.Gray else MaterialTheme.colorScheme.primary, // 로딩 중 색상 변경
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "인증문자 재발송",
                    color = if (isLoading) Color.Gray else MaterialTheme.colorScheme.primary, // 로딩 중 색상 변경
                    fontSize = 14.sp
                )
            }


            Spacer(modifier = Modifier.weight(1f))

            // Submit button
            Button(
                onClick = {
                    if (enteredCode.length == codeLength) {
                        verifyCodeAction(enteredCode) // <<< 내부 함수 호출
                    }
                },
                enabled = enteredCode.length == codeLength && !isLoading, // 로딩 중 아닐 때만 활성화
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("인증 확인")
                }
            }
        }
    }
}