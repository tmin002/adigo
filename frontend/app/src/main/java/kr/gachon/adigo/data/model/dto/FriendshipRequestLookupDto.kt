package kr.gachon.adigo.data.model.dto

data class FriendshipRequestLookupDto(
    val id: Long,
    val requesterName: String,
    val requesterEmail: String,
    val addresseeEmail: String,
    val status: String
) 