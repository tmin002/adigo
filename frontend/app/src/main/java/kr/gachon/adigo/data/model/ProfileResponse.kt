package kr.gachon.adigo.data.model

data class ProfileResponse(
    val status: Int,
    val message: String,
    val data: UserProfile
)
