package kr.gachon.adigo.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

// 바텀시트에 표시할 컨텐츠 종류를 구분하기 위한 enum
enum class BottomSheetContentType {
    FRIENDS, MYPAGE, SETTINGS
}

class MainScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            PersistentBottomSheetMapScreen(navController = navController)
        }
    }
}

@Composable
fun PersistentBottomSheetMapScreen(
    navController: NavController,
    initialContent: BottomSheetContentType = BottomSheetContentType.FRIENDS,
    initialBottomSheetValue: BottomSheetValue = BottomSheetValue.Collapsed
) {
    // BottomSheetScaffold 상태와 코루틴 스코프
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = initialBottomSheetValue)
    )
    val scope = rememberCoroutineScope()
    var selectedContent by remember { mutableStateOf(initialContent) }

    // 예시: 서울 좌표
    val seoul = LatLng(37.56, 126.97)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 10f)
    }

    // Box로 전체 레이아웃 감싸고, 고정 버튼 바를 오버레이로 배치합니다.
    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            // 시트가 펼쳐질 때 시작 위치가 고정 버튼 바 높이(56.dp)에서 시작하도록 설정
            sheetPeekHeight = 144.dp,
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp) // 필요에 따라 높이 조절
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    // 드래그 핸들 (시트 상단에 표시)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Gray.copy(alpha = 0.7f))
                        )
                    }
                    // 선택된 컨텐츠에 따라 다른 내용을 표시
                    when (selectedContent) {
                        BottomSheetContentType.FRIENDS -> {
                            Text(text = "친구 목록", style = MaterialTheme.typography.h6)
                            Spacer(modifier = Modifier.height(8.dp))
                            for (i in 1..10) {
                                Text(text = "친구 $i")
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        BottomSheetContentType.MYPAGE -> {
                            Text(text = "마이페이지", style = MaterialTheme.typography.h6)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "여기에 마이페이지 관련 정보를 표시합니다.")
                        }
                        BottomSheetContentType.SETTINGS -> {
                            Text(text = "설정", style = MaterialTheme.typography.h6)
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "알림 설정",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("notification_setting")
                                        }
                                        .padding(vertical = 16.dp, horizontal = 8.dp),
                                    color = Color.Black
                                )
                                Divider(color = Color.LightGray, thickness = 1.dp)

                                Text(
                                    text = "회원가입 정보",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("signup_info")
                                        }
                                        .padding(vertical = 16.dp, horizontal = 8.dp),
                                    color = Color.Black
                                )
                                Divider(color = Color.LightGray, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            // Main content 영역: 지도 화면
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = seoul),
                        title = "Seoul",
                        snippet = "Marker in Seoul"
                    )
                }
            }
        }
        // 고정된 하단 버튼 바를 Box의 자식으로 오버레이로 배치 (zIndex를 높여 시트 위에 고정)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.White)
                .zIndex(1f), // 시트보다 위에 표시되도록
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FixedBottomButton(
                icon = Icons.Default.Person,
                label = "친구",
                onClick = {
                    selectedContent = BottomSheetContentType.FRIENDS
                    scope.launch { scaffoldState.bottomSheetState.expand() }
                }
            )
            FixedBottomButton(
                icon = Icons.Default.AccountCircle,
                label = "마이페이지",
                onClick = {
                    selectedContent = BottomSheetContentType.MYPAGE
                    scope.launch { scaffoldState.bottomSheetState.expand() }
                }
            )
            FixedBottomButton(
                icon = Icons.Default.Settings,
                label = "설정",
                onClick = {
                    selectedContent = BottomSheetContentType.SETTINGS
                    scope.launch { scaffoldState.bottomSheetState.expand() }
                }
            )
        }

    }
}

// 하단 고정 버튼 컴포저블 (아이콘 + 텍스트)
@Composable
fun FixedBottomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colors.primary
        )
        Text(text = label, style = MaterialTheme.typography.caption)
    }
}
