package kr.gachon.adigo.data.local.transformer

import kr.adigo.adigo.database.entity.ChatBubbleEntity
import kr.adigo.adigo.database.entity.ChatRoomEntity
import kr.gachon.adigo.data.model.global.ChatBubble
import kr.gachon.adigo.data.model.global.ChatRoom

object ChatRoomTransformer: BasedTransformer<ChatRoom, ChatRoomEntity> {
    override fun modelToEntity(model: ChatRoom): ChatRoomEntity {
        TODO("Not yet implemented")
    }

    override fun entityToModel(entity: ChatRoomEntity): ChatRoom {
        TODO("Not yet implemented")
    }

}