package kr.gachon.adigo.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

// 메인 액티비티
class MainScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersistentBottomSheetMapScreen()
        }
    }
}

// 바텀시트에 표시할 컨텐츠 종류를 구분하기 위한 enum
enum class BottomSheetContentType {
    FRIENDS, MYPAGE, SETTINGS
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PersistentBottomSheetMapScreen() {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
    )
    val scope = rememberCoroutineScope()

    // 선택된 컨텐츠 상태 (기본값: FRIENDS)
    var selectedContent by remember { mutableStateOf(BottomSheetContentType.FRIENDS) }

    // 예시: 서울 좌표
    val seoul = LatLng(37.56, 126.97)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 10f)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        // 시트가 기본적으로 보이지 않게 peek 높이를 0.dp로 설정
        sheetPeekHeight = 0.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // 필요에 따라 높이 조절
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(16.dp)
            ) {
                // 드래그 핸들 (사용자가 시트를 드래그할 수 있도록)
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
                // 선택된 버튼에 따른 컨텐츠 분기
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
                        Text(text = "여기에 앱 설정 관련 정보를 표시합니다.")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 지도 영역
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
            // 고정된 하단 버튼 바 (content 영역에 배치하면 시트가 위로 올라올 때 덮어집니다)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FixedBottomButton(
                    icon = Icons.Default.Person,
                    label = "친구",
                    onClick = {
                        selectedContent = BottomSheetContentType.FRIENDS
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                )
                FixedBottomButton(
                    icon = Icons.Default.AccountCircle,
                    label = "마이페이지",
                    onClick = {
                        selectedContent = BottomSheetContentType.MYPAGE
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                )
                FixedBottomButton(
                    icon = Icons.Default.Settings,
                    label = "설정",
                    onClick = {
                        selectedContent = BottomSheetContentType.SETTINGS
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                )
            }
        }
    }
}

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
