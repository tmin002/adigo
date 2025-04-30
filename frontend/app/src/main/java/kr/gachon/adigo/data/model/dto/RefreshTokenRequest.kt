package kr.gachon.adigo.data.model.dto

data class RefreshTokenRequest(
    val accessToken: String,
    val refreshToken: String
)
