package kr.gachon.adigo.data.model.dto

import kr.gachon.adigo.data.model.global.User

data class FriendListResponse(
    val status: Int,
    val message: String,
    val data: List<User>
    )



