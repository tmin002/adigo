package kr.gachon.adigo.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.ui.viewmodel.FriendListViewModel
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items   // ← 추가
import androidx.compose.material.AlertDialog
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.rememberDismissState
import kr.adigo.adigo.database.entity.UserEntity
import androidx.compose.material.Switch
import androidx.compose.material.Divider
import androidx.compose.material.ButtonDefaults
import kr.gachon.adigo.data.model.dto.FriendshipRequestLookupDto
import android.util.Log


// ===============================
// Friends 탭 내부 (3가지 화면)
// ===============================

@Composable
fun FriendsBottomSheetContent(
    friendScreenState: FriendScreenState,
    onSelectFriend: (String) -> Unit,
    onClickBack: () -> Unit,
    friendlistviewModel: FriendListViewModel = remember {
        FriendListViewModel(repo = AdigoApplication.AppContainer.userDatabaseRepo)
    }
) {
    val friends by friendlistviewModel.friends.collectAsState(emptyList())
    val friendRequests by friendlistviewModel.friendRequests.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { 
        Log.d("BottomSheetContent", "LaunchedEffect triggered")
        friendlistviewModel.refreshFriends()
        friendlistviewModel.refreshFriendRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        DragHandle()

        when (friendScreenState) {
            is FriendScreenState.List -> {
                Header(onAddFriendClick = { showAddDialog = true })

                // 친구 요청 목록
                if (friendRequests.isNotEmpty()) {
                    Text(
                        text = "친구 요청",
                        style = MaterialTheme.typography.h6,
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
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // 친구 목록
                Text(
                    text = "친구 목록",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn {
                    items(friends, key = { it.id }) { user ->
                        FriendListItem(
                            user = user,
                            onClick = { onSelectFriend(user.email) },
                            onDelete = { friendlistviewModel.deleteFriend(user) }
                        )
                    }
                }
            }
            is FriendScreenState.Profile -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "친구 프로필: ${friendScreenState.friendId}",
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = "뒤로",
                        modifier = Modifier.clickable { onClickBack() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "이곳에 친구 프로필 상세 정보를 표시합니다.")
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
private fun AddFriendDialog(
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
fun MyPageBottomSheetContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            DragHandle()
            Spacer(modifier = Modifier.height(8.dp))

            // 위치 정보 그룹
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F2F7))
                    .padding(16.dp)
            ) {
                Text("나의 위치", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(8.dp))
                Text("위치: 대한민국, 경기도", style = MaterialTheme.typography.body1)
                Text("기기: 이 Android", style = MaterialTheme.typography.body1)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("나의 위치 공유", style = MaterialTheme.typography.body1)
                    Switch(checked = true, onCheckedChange = {})
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 알림 그룹
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F2F7))
                    .padding(16.dp)
            ) {
                Text("알림", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("친구 요청 허용", style = MaterialTheme.typography.body1)
                    Switch(checked = true, onCheckedChange = {})
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 링크 그룹
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F2F7))
                    .padding(16.dp)
            ) {
                Text("'나의 찾기' 알림 사용자화", color = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("추적 알림 사용자화", color = MaterialTheme.colors.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 하단 텍스트
            Text(
                "친구 돕기",
                color = MaterialTheme.colors.primary,
//                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun SettingsBottomSheetContent(
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            DragHandle()
            Text(text = "설정", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "여기에 앱 설정 관련 정보를 표시합니다.")
            Text(
                text = "로그아웃",
                style = MaterialTheme.typography.button,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colors.error.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable {  onLogout()  }  // ← 여기서 호출
            )
        }
    }
}

// 바텀시트 상단의 드래그 핸들
@Composable
fun DragHandle() {
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
}

@Composable
private fun Header(onAddFriendClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "친구 목록", style = MaterialTheme.typography.h6)

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
private fun FriendListItem(
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
        // 왼쪽 – 아바타(이니셜)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(user.name.first().uppercaseChar().toString())
        }

        Spacer(Modifier.width(12.dp))

        // 가운데 – 이름
        Text(
            text = user.name,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.weight(1f)     // 남은 공간 사용
        )

        // 오른쪽 – 삭제 아이콘
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "삭제",
                tint = MaterialTheme.colors.error
            )
        }
    }
}

@Composable
private fun FriendRequestItem(
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
                .background(MaterialTheme.colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(request.requesterName.first().uppercaseChar().toString())
        }

        Spacer(Modifier.width(12.dp))

        // 가운데 – 이름
        Text(
            text = request.requesterName,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.weight(1f)
        )

        // 오른쪽 – 수락/거절 버튼
        Row {
            TextButton(
                onClick = onAccept,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colors.primary
                )
            ) {
                Text("수락")
            }
            TextButton(
                onClick = onReject,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colors.error
                )
            ) {
                Text("거절")
            }
        }
    }
}
