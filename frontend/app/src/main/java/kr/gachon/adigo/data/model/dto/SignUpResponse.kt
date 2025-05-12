package kr.gachon.adigo.data.model.dto

data class SignUpResponse(
    val status: Int,
    val message: String,
    val data: Response
) {
    data class Response(
        val email: String,
        val nickname: String,
        val name: String
    )
}
