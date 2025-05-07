package kr.gachon.adigo.data.local.transformer

import kr.adigo.adigo.database.entity.ChatBubbleEntity
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.data.model.global.ChatBubble
import kr.gachon.adigo.data.model.global.User


/**
 * User ↔︎ UserEntity 매핑 전담
 *
 * * DB(Realm) → Model(도메인) : [entityToModel]
 * * Model → DB : [modelToEntity]
 *
 * **주의**
 * * `profileImage` 가 null 이면 빈 문자열로 저장해 Realm `null` 제한 피함.
 */
object UserTransformer : BasedTransformer<User, UserEntity> {

    /** Model → Realm Entity */
    override fun modelToEntity(model: User): UserEntity =
        UserEntity().apply {
            id = model.id                      // 이메일 = PK
            email = model.email
            name = model.nickname
            profileImageURL = model.profileImage ?: ""
            authority = model.authority.name

        }

    /** Realm Entity → Model */
    override fun entityToModel(entity: UserEntity): User =
        User(
            email        = entity.email,
            nickname     = entity.name,
            profileImage = entity.profileImageURL.takeIf { it.isNotBlank() },
            authority    = User.Authority.valueOf(entity.authority)


        )
}