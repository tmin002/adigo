package kr.gachon.adigo.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class MainScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersistentBottomSheetMapScreen()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PersistentBottomSheetMapScreen() {
    // BottomSheetScaffold 상태 생성: 기본값은 Collapsed 상태
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
    )
    val scope = rememberCoroutineScope()

    // 예시로 서울 좌표 사용
    val seoul = LatLng(37.56, 126.97)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 10f)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        // persistent bottom sheet 내용
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)  // 시트가 확장되었을 때의 높이
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .fillMaxWidth()
                        .height(104.dp)
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    Button(
                        onClick = { /* 친구 버튼 */},
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(text = "친구")
                    }
                    Button(
                        onClick = { /* 마이페이지 버튼 */},
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(text = "마이페이지")
                    }
                    Button(
                        onClick = { /* 설정 버튼 */},
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(text = "설정")
                    }
                }
                Text(
                    text = "친구 목록",
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 예시 친구 목록
                for (i in 1..10) {
                    Text(text = "친구 $i")
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        // 시트가 축소된(peek) 상태일 때의 높이 설정
        sheetPeekHeight = 120.dp
    ) { innerPadding ->
        // 메인 콘텐츠: 지도 화면
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
}
