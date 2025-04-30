package kr.gachon.adigo.data.model.dto

data class LoginResponse(
    val status: Int,
    val message: String,
    val data: Response
) {
    data class Response(
        val grantType: String,
        val accessToken: String,
        val refreshToken: String,
        val tokenExpiresIn: String,
        val refreshTokenExpiresIn: String
    )
}

