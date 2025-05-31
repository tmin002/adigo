package kr.gachon.adigo.data.model.dto

data class ResetPasswordDto(
    val email: String,
    val password: String,
    val passwordConfirm: String
) 