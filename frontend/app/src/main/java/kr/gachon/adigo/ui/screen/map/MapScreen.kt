package kr.gachon.adigo.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.BottomSheetValue.Collapsed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MapScreen() {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = Collapsed)
    )
    val scope = rememberCoroutineScope()

    var selectedContent by remember { mutableStateOf(BottomSheetContentType.FRIENDS) }
    var friendScreenState by remember { mutableStateOf<FriendScreenState>(FriendScreenState.List) }

    // 지도 카메라 위치 예시 (서울)
    val seoul = LatLng(37.56, 126.97)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 10f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 144.dp,
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetContent = {
                when (selectedContent) {
                    BottomSheetContentType.FRIENDS -> {
                        FriendsBottomSheetContent(
                            friendScreenState = friendScreenState,
                            onSelectFriend = { friendId ->
                                friendScreenState = FriendScreenState.Profile(friendId)
                            },
                            onClickManage = {
                                friendScreenState = FriendScreenState.Manage
                            },
                            onClickBack = {
                                friendScreenState = FriendScreenState.List
                            }
                        )
                    }
                    BottomSheetContentType.MYPAGE -> {
                        MyPageBottomSheetContent()
                    }
                    BottomSheetContentType.SETTINGS -> {
                        SettingsBottomSheetContent()
                    }
                }
            }
        ) { innerPadding ->
            // 지도 부분
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

        // 하단 고정 버튼 바
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.White)
                .zIndex(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FixedBottomButton(
                icon = Icons.Default.Person,
                label = "친구",
                onClick = {
                    selectedContent = BottomSheetContentType.FRIENDS
                    friendScreenState = FriendScreenState.List
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
