package kr.gachon.adigo.data.model.dto

data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String,
    val phonenumber: String
)
