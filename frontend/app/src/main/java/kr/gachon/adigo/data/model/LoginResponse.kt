package kr.gachon.adigo.data.model

data class LoginResponse(
    val jwtToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null  // 토큰 만료시간 추가 (선택적)
)
