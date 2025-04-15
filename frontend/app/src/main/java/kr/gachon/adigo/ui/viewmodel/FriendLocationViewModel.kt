package kr.gachon.adigo.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kr.gachon.adigo.data.model.global.UserLocation

class FriendLocationViewModel  : ViewModel(){
    val friends = mutableStateListOf<UserLocation>()
    init {
        // 예시: 초기 친구 목록 하드코딩 (실제에선 서버에서 받아옴)
        friends.addAll(
            listOf(
                UserLocation("철수", 37.56, 126.97),
                UserLocation("영희", 37.55, 126.98),
                UserLocation("민수", 37.54, 126.96)
            )
        )
    }
}