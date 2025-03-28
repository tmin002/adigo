package kr.gachon.adigo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.remote.httpClient
import kr.gachon.adigo.ui.screen.MainScreenActivity
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


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun VerificationCodeScreen(
        onBackPress: () -> Unit,
        onResendClick: () -> Unit
    ) {
        // --- State ---
        // Example Timer State (Replace with your actual timer logic)
        var timeLeft by remember { mutableStateOf(2 * 60 + 52) } // 2:52 in seconds
        val timerText = remember(timeLeft) {
            val minutes = timeLeft / 60
            val seconds = timeLeft % 60
            "%02d:%02d".format(minutes, seconds)
        }

        // Example Timer Countdown Logic
        LaunchedEffect(key1 = timeLeft) {
            if (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            // Optionally handle timer expiration (e.g., disable input, change resend text)
        }

        // --- System Back Button Handling ---
        BackHandler {
            onBackPress()
        }

        // --- UI ---
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { /* No title needed */ },
                    navigationIcon = {
                        IconButton(onClick = onBackPress) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로 가기" // Back button
                            )
                        }
                    },
                    // Make TopAppBar transparent if needed
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Or MaterialTheme.colorScheme.background
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground // Adjust color as needed
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // Apply padding from Scaffold
                    .padding(horizontal = 24.dp, vertical = 16.dp), // Add screen padding
                verticalArrangement = Arrangement.spacedBy(24.dp) // Space between elements
            ) {
                // --- Title ---
                Text(
                    text = "보내드린 인증번호 6자리를\n입력해주세요", // Enter the 6-digit verification code sent
                    fontSize = 22.sp, // Adjust size as needed
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp // Adjust line height for better readability
                )

                // --- Timer ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp) // Add some space above timer
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBox, // Or Icons.Filled.AccessTime
                        contentDescription = "남은 시간", // Remaining time
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timerText,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                // --- Input Placeholders ---
                // This is a visual placeholder. You'll need a real OTP input field
                // implementation (e.g., using BasicTextField or a library).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp), // Add vertical padding
                    horizontalArrangement = Arrangement.SpaceBetween // Distribute space evenly
                ) {
                    repeat(6) {
                        Divider(
                            modifier = Modifier
                                .width(40.dp) // Adjust width of the line
                                .height(2.dp),
                            color = MaterialTheme.colorScheme.primary // Use theme color (blue)
                        )
                    }
                }
                // Note: Add your actual OTP input field composable here


                // --- Resend Button ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onResendClick) // Make the row clickable
                        .padding(vertical = 8.dp) // Add padding for better touch area
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null, // Text describes the action
                        tint = MaterialTheme.colorScheme.primary, // Blue color
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "인증문자 재발송", // Resend verification code
                        color = MaterialTheme.colorScheme.primary, // Blue color
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Pushes the content up if there's extra space
                Spacer(modifier = Modifier.weight(1f))
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
                            navController.navigate(Screens.VerifyCode.name)

                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("다음")
                    }
                }
            }
        }


    }

    @Composable
    fun onBoard(viewModel: AuthViewModel, navController: NavController) {

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
                onBoard(viewModel, navController)
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
                    onBackPress = {
                        navController.popBackStack()
                    }
                ) {

                }
            }
            composable(route = Screens.Main.name) {
                //Main()
            }


        }


    }


}
