package kr.gachon.adigo.ui.screen.map

// BottomSheet의 대분류
enum class BottomSheetContentType {
    FRIENDS, MYPAGE, SETTINGS
}

// FRIENDS 탭 내부에서 세부 화면 전환용
sealed class FriendScreenState {
    object List : FriendScreenState()
    data class Profile(val friendId: String) : FriendScreenState()
    object Manage : FriendScreenState()
}
