package kr.gachon.adigo.data.model.dto.webauthn

data class AssertionResponseDto(
        val requestId: String,
        val credential: String // CredentialManager에서 받은 JSON 문자열 전체
)