package kr.gachon.adigo.data.model

data class UserProfile(
    val email: String,
    val nickname: String,
    val profileImage: String?,  // Swift의 URL? -> String?
    val authority: Authority
) {

    // Swift의 enum Authority -> Kotlin enum class
    enum class Authority {
        ROLE_ADMIN,
        ROLE_USER
    }
}
