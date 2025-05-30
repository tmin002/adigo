package kr.gachon.adigo.data.model.global

import java.time.LocalDateTime

data class User(
    override val id: Long,
    val email: String,
    val nickname: String,
    val profileImage: String?,
    val authority: Authority,
    val isOnline: Boolean,
    val lastSeen: LocalDateTime?
) : BasedManagedModel<UserDTO> {

    override fun getDTO(): UserDTO {
        return UserDTO(id, nickname, email, profileImage, authority, isOnline, lastSeen)
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
    val authority: User.Authority,
    val isOnline: Boolean,
    val lastSeen: LocalDateTime?
) : BasedDataTransfterObject