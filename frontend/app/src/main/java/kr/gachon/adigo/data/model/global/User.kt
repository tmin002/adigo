package kr.gachon.adigo.data.model.global

data class User(
    override val id: Long,
    val email: String,
    val nickname: String,
    val profileImage: String?,  // Swift의 URL? -> String?
    val authority: Authority
): BasedManagedModel<UserDTO> {
    //var location: UserLocation? = null

    override fun getDTO(): UserDTO {
        return UserDTO(id, nickname, email, profileImage, authority)
    }

    enum class Authority {
        ROLE_ADMIN,
        ROLE_USER
    }
}
class UserDTO(
    override val id: Long,
    val nickname: String,
    val email: String,
    val profileImage: String?,
    val authority: User.Authority

): BasedDataTransfterObject