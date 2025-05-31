package kr.gachon.adigo.ui.screen // 패키지 경로는 실제 프로젝트에 맞게 조정

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // 비밀번호 가려짐 상태 저장 위해
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import kr.gachon.adigo.ui.screen.Screens // 네비게이션 경로 위해 추가 (예시)

@OptIn(ExperimentalMaterial3Api::class) // CenterAlignedTopAppBar 등 실험적 API 사용 시
@Composable
fun FinalSignUpScreen(
    viewModel: AuthViewModel,
    Email: String, // 이전 화면에서 전달받은 이메일
    phonenumber: String, // 이전 화면에서 전달받은 전화번호
    navController: NavController // 네비게이션 처리 위해 변경 (navcontroller -> navController)
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // 비밀번호 일치 여부 확인
    val isPasswordMatching by remember(password, confirmPassword) {
        derivedStateOf {
            password == confirmPassword
        }
    }

    // 가입 처리 함수
    val performSignUp: () -> Unit = { // 람다 시작

        // 간단한 유효성 검사 (필요에 따라 강화)
        if (name.isBlank() || password.isBlank()) {
            errorMessage = "이름과 비밀번호를 모두 입력해주세요."
            // return@FinalSignUpScreen // <<< 문제의 코드

        }

        isLoading = true
        errorMessage = null
        Log.d("FinalSignUpScreen", "Attempting final sign up for email: $Email, phone: $phonenumber")

        // ViewModel의 최종 가입 함수 호출 (함수 이름은 실제 ViewModel에 맞게 수정)
        viewModel.signUp( // 함수 이름 확인 필요 (이전엔 performFinalSignUp 예시 사용)
            email = Email,
            phone = phonenumber,
            name = name,
            password = password,
            onSuccess = {
                isLoading = false
                Log.i("FinalSignUpScreen", "Sign up successful!")
                Toast.makeText(context, "회원가입 성공!", Toast.LENGTH_SHORT).show() // 성공 Toast 추가
                // 가입 성공 후 메인 화면 등으로 이동
                navController.navigate(Screens.Main.name) { // 성공 시 네비게이션 경로 수정 필요

                    popUpTo(Screens.OnBoard.name) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onError = { errorMsg ->
                isLoading = false
                Log.e("FinalSignUpScreen", "Sign up failed: $errorMsg")
                errorMessage = "가입 실패: $errorMsg"
            }
        )
    } // 람다 끝

    // UI 구성
    Scaffold(
        topBar = {
            // 필요하다면 TopAppBar 추가 (뒤로가기 등)
            CenterAlignedTopAppBar(title = { Text("회원가입") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "마지막 단계입니다!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "사용하실 이름과 비밀번호를 입력해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp)) // 여백 추가

            // 이름 입력 필드
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("이름") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null // 에러 시 시각적 피드백 (선택 사항)
            )

            // 비밀번호 입력 필드
            OutlinedTextField(
                value = password,
                onValueChange = { newValue -> password = newValue },
                label = { Text("비밀번호") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Lock
                    else Icons.Filled.Lock
                    val description = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                },
                isError = errorMessage != null,// 에러 시 시각적 피드백 (선택 사항)
                //비밀번호 자동완성 없애기
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus() // 키보드 완료 버튼 누르면 포커스 해제
                    }
                )
            )

            // 비밀번호 확인 입력 필드
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { newValue -> confirmPassword = newValue },
                label = { Text("비밀번호 확인") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmPasswordVisible)
                        Icons.Filled.Lock
                    else Icons.Filled.Lock
                    val description = if (confirmPasswordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, description)
                    }
                },
                isError = !isPasswordMatching && confirmPassword.isNotEmpty(),
                //비밀번호 자동완성 없애기
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus() // 키보드 완료 버튼 누르면 포커스 해제
                    }
                )
            )

            // 비밀번호 불일치 에러 메시지
            if (!isPasswordMatching && confirmPassword.isNotEmpty()) {
                Text(
                    text = "비밀번호가 일치하지 않습니다.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 에러 메시지 표시
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth() // 너비 채우도록
                )
            }

            Spacer(modifier = Modifier.weight(1f)) // 버튼을 하단으로 밀기

            // 가입하기 버튼
            Button(
                onClick = performSignUp, // 내부 함수 호출
                enabled = !isLoading && isPasswordMatching && password.isNotEmpty() && confirmPassword.isNotEmpty(), // 비밀번호가 일치하고 둘 다 입력되었을 때만 활성화
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary // 버튼 위 로딩 색상
                    )
                } else {
                    Text("가입하기")
                }
            }
        }
    }
}