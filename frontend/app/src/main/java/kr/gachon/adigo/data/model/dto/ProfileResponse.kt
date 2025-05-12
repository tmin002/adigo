package kr.gachon.adigo.data.model.dto

import kr.gachon.adigo.data.model.global.User


data class ProfileResponse(
    val status: Int,
    val message: String,
    val data: User
)
