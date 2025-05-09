package kr.gachon.adigo.data.model.dto

import kr.gachon.adigo.data.model.global.UserLocationDto

// Matches the JSON structure for /user/queue/friendsLocationResponse
data class FriendsLocationResponseDto(
    val friends: List<UserLocationDto>
)