package kr.gachon.adigo.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ===============================
// Friends 탭 내부 (3가지 화면)
// ===============================
@Composable
fun FriendsBottomSheetContent(
    friendScreenState: FriendScreenState,
    onSelectFriend: (String) -> Unit,
    onClickManage: () -> Unit,
    onClickBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        // 드래그 핸들
        DragHandle()

        when (friendScreenState) {
            is FriendScreenState.List -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "친구 목록", style = MaterialTheme.typography.h6)
                    Text(
                        text = "관리",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.clickable { onClickManage() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 예시 친구 목록
                for (i in 1..10) { //친구 db 가져오기
                    Text(
                        text = "친구 $i",
                        modifier = Modifier
                            .clickable {
                                // 프로필로 이동
                                onSelectFriend("friend_$i")
                            }
                            .padding(8.dp)
                    )
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
            is FriendScreenState.Manage -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "친구 관리", style = MaterialTheme.typography.h6)
                    Text(
                        text = "뒤로",
                        modifier = Modifier.clickable { onClickBack() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "여기에 친구 추가 / 친구 요청 확인 / 차단 관리 등을 표시합니다.")
            }
        }
    }
}

@Composable
fun MyPageBottomSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        DragHandle()
        Text(text = "마이페이지", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "여기에 마이페이지 관련 정보를 표시합니다.")
    }
}

@Composable
fun SettingsBottomSheetContent(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(16.dp)
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
        Spacer(modifier = Modifier.weight(1f))


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
