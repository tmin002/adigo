package kr.gachon.adigo.data.local.transformer

import kr.adigo.adigo.database.entity.ChatBubbleEntity
import kr.gachon.adigo.data.model.global.ChatBubble

object ChatBubbleTransformer: BasedTransformer<ChatBubble, ChatBubbleEntity> {
    override fun modelToEntity(model: ChatBubble): ChatBubbleEntity {
        TODO("Not yet implemented")
    }

    override fun entityToModel(entity: ChatBubbleEntity): ChatBubble {
        TODO("Not yet implemented")
    }

}