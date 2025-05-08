package kr.gachon.adigo.data.model.dto

// Matches the JSON structure for /user/queue/friendsLocationResponse
data class FriendsLocationResponseDto(
    val friends: List<FriendLocationDto>
)