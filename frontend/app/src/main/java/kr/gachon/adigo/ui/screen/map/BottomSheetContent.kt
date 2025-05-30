package kr.gachon.adigo.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.ui.viewmodel.FriendListViewModel
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.data.model.dto.FriendshipRequestLookupDto
import android.util.Log
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kr.gachon.adigo.data.remote.websocket.StompWebSocketClient
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import kr.gachon.adigo.ui.viewmodel.MyPageViewModel
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


// ===============================
// Friends 탭 내부 (3가지 화면)
// ===============================

@Composable
fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
        )
    }
}

@Composable
fun MyPageBottomSheetContent() {
    // Get WebSocket components from Application
    val stompClient = remember { AdigoApplication.AppContainer.stompClient }
    val locationReceiver = remember { AdigoApplication.AppContainer.wsReceiver }

    val viewModel = remember { MyPageViewModel(AdigoApplication.AppContainer.userDatabaseRepo) }
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    // 이미지 선택 결과 처리
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfileImage(it, context) }
    }

//    // 에러 다이얼로그
//    error?.let { errorMessage ->
//        AlertDialog(
//            onDismissRequest = { viewModel.clearError() },
//            title = { Text("오류") },
//            text = { Text(errorMessage) },
//            confirmButton = {
//                TextButton(onClick = { viewModel.clearError() }) {
//                    Text("확인")
//                }
//            }
//        )
//    }

    var showEditDialog by remember { mutableStateOf(false) }
    var newNickname by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp)
    ) {
        DragHandle()
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            item {
                // 프로필 섹션
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 프로필 이미지와 닉네임을 포함하는 Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 프로필 이미지
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(60.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .clickable(enabled = !isLoading) { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (currentUser?.profileImageURL?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = currentUser?.profileImageURL,
                                    contentDescription = "프로필 이미지",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = currentUser?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 닉네임과 수정 버튼을 포함하는 Column
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentUser?.name ?: "",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                IconButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "닉네임 수정",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // 닉네임 수정 다이얼로그
                if (showEditDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditDialog = false },
                        title = { Text("닉네임 수정") },
                        text = {
                            Column {
                                TextField(
                                    value = newNickname,
                                    onValueChange = { newNickname = it },
                                    singleLine = true,
                                    placeholder = { Text("새로운 닉네임을 입력하세요") }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (newNickname.isNotBlank()) {
                                        viewModel.updateNickname(newNickname)
                                        showEditDialog = false
                                    }
                                }
                            ) {
                                Text("저장")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditDialog = false }) {
                                Text("취소")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // WebSocket Status Group
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Text("연결 상태", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // WebSocket Connection Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("서버 연결", style = MaterialTheme.typography.bodyLarge)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (stompClient.stompConnected) Color.Green else Color.Red,
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Friend Location Subscription Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("친구 위치 구독", style = MaterialTheme.typography.bodyLarge)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(

                                    color = if (stompClient.stompConnected && locationReceiver.listenJob?.isActive == true) Color.Green else Color.Red,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 위치 정보 그룹
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Text("나의 위치", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("위치: 대한민국, 경기도", style = MaterialTheme.typography.bodyLarge)
                    Text("기기: 이 Android", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsBottomSheetContent(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    val versionCode = packageInfo.longVersionCode

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp)
    ) {
        DragHandle()
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "설정", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "여기에 앱 설정 관련 정보를 표시합니다.")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "앱 버전: $versionName (빌드 $versionCode)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "로그아웃",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { onLogout() }
        )
    }
}



@Composable
fun AddFriendDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("친구 추가") },
        text = {
            Column {
                Text("추가할 친구의 이메일을 입력하세요.")
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    placeholder = { Text("example@email.com") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { onAdd(email) }
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}





@Composable
fun FriendsBottomSheetContent(
    friendScreenState: FriendScreenState,
    onSelectFriend: (UserEntity) -> Unit,
    onNavigateToFriend: (UserEntity) -> Unit,
    onClickBack: () -> Unit,
) {
    val friendlistviewModel = remember { FriendListViewModel() }
    val friends by friendlistviewModel.friends.collectAsState(emptyList())
    val friendRequests by friendlistviewModel.friendRequests.collectAsState(emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    val friendLocations by AdigoApplication.AppContainer.userLocationRepo.friends.collectAsState(emptyList())
    val myPageViewModel = remember { MyPageViewModel(AdigoApplication.AppContainer.userDatabaseRepo) }

    LaunchedEffect(Unit) {
        Log.d("BottomSheetContent", "LaunchedEffect triggered")
        friendlistviewModel.refreshFriends()
        friendlistviewModel.refreshFriendRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp)
    ) {
        DragHandle()

        when (friendScreenState) {
            is FriendScreenState.List -> {
                Header(onAddFriendClick = { showAddDialog = true })

                // 친구 요청 목록
                if (friendRequests.isNotEmpty()) {
                    Text(
                        text = "친구 요청",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyColumn {
                        items(friendRequests) { request ->
                            FriendRequestItem(
                                request = request,
                                onAccept = { friendlistviewModel.replyToFriendRequest(request.requesterEmail, true) },
                                onReject = { friendlistviewModel.replyToFriendRequest(request.requesterEmail, false) }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // 친구 목록
                Text(
                    text = "친구 목록",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn {
                    items(friends) { user ->
                        FriendListItem(
                            user = user,
                            onClick = { onSelectFriend(user) },
                            onDelete = { friendlistviewModel.deleteFriend(user) }
                        )
                    }
                }
            }
            is FriendScreenState.Profile -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 헤더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "친구 프로필",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "뒤로",
                            modifier = Modifier.clickable { onClickBack() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 프로필 정보
                    val friend = friendScreenState.friend
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 프로필 이미지
                        if (friend.profileImageURL.isNotEmpty()) {
                            AsyncImage(
                                model = friend.profileImageURL,
                                contentDescription = "프로필 이미지",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(50.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = friend.name.first().uppercaseChar().toString(),
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 이름
                        Text(
                            text = friend.name,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 이메일
                        Text(
                            text = friend.email,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 길 찾기 버튼
                        Button(
                            onClick = { onNavigateToFriend(friend) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "길 찾기",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFriendDialog(
            onAdd = { email ->
                friendlistviewModel.addFriend(email)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun Header(onAddFriendClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "친구 추가", style = MaterialTheme.typography.titleLarge)

        IconButton(onClick = onAddFriendClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "친구 추가"
            )
        }

    }
    Spacer(modifier = Modifier.height(8.dp))
}


@Composable
fun FriendListItem(
    user: UserEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 프로필 이미지
        if (user.profileImageURL.isNotEmpty()) {
            AsyncImage(
                model = user.profileImageURL,
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(user.name.first().uppercaseChar().toString())
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 이름
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium
            )

            // 접속 상태 텍스트
            Text(
                text = if (user.isOnline) "🟢 온라인" else "⚪ 마지막 접속: ${formatLastSeen(user.lastSeenString)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (user.isOnline) Color(0xFF4CAF50) else Color.Gray
            )
        }

        // 삭제 아이콘
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "삭제",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}



@Composable
fun FriendRequestItem(
    request: FriendshipRequestLookupDto,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽 – 아바타(이니셜)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(request.requesterName.first().uppercaseChar().toString())
        }

        Spacer(Modifier.width(12.dp))

        // 가운데 – 이름
        Text(
            text = request.requesterName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        // 오른쪽 – 수락/거절 버튼
        Row {
            TextButton(
                onClick = onAccept,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("수락")
            }
            TextButton(
                onClick = onReject,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("거절")
            }
        }
    }
}

fun formatLastSeen(raw: String?): String {
    return try {
        if (raw.isNullOrBlank()) return "알 수 없음"
        val parsed = LocalDateTime.parse(raw)
        parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (e: Exception) {
        "알 수 없음"
    }
}




