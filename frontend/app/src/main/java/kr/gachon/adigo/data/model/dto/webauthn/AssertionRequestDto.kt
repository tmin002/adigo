package kr.gachon.adigo.data.model.dto.webauthn

data class AssertionRequestDto(
    val requestId: String,
    val publicKeyCredentialRequestOptions: Map<String, Any> // JSON 전체를 그대로 받음
)

