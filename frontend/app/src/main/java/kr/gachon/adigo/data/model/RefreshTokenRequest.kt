package kr.gachon.adigo.data.model

data class RefreshTokenRequest(
    val accessToken: String,
    val refreshToken: String
)
