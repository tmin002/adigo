package kr.gachon.adigo.data.local.transformer

import kr.adigo.adigo.database.entity.ChatBubbleEntity
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.data.model.global.ChatBubble
import kr.gachon.adigo.data.model.global.User

object UserTransformer: BasedTransformer<User, UserEntity> {
    override fun modelToEntity(model: User): UserEntity {
        TODO("Not yet implemented")
    }

    override fun entityToModel(entity: UserEntity): User {
        TODO("Not yet implemented")
    }

}