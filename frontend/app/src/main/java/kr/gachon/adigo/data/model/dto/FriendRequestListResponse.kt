package kr.gachon.adigo.data.model.dto

data class FriendRequestListResponse(
    val status: Int,
    val message: String,
    val data: List<FriendshipRequestLookupDto>
) 