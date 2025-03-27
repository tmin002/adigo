package kr.gachon.adigo.data.model.global
import java.time.LocalDateTime

data class ChatBubble(
    override val id: String,
    val message: String,
    val type: ChatBubbleType
): IManagedModel<ChatRoomDTO> {
    override fun getDTO(): ChatRoomDTO {
        TODO("Not yet implemented. 채팅 기능 수정 필요")
    }
    fun fromDTO(dto: ChatBubbleDTO): ChatBubble {
        return ChatBubble(dto.id, dto.sender, dto.type)
    }
}

enum class ChatBubbleType {
    ENTER, SEND
}

class ChatBubbleDTO(
    val message_id: Long,
    val type: ChatBubbleType,
    val roomId: String,
    val sender: String,
    val message: String,
    val time: LocalDateTime

) : IDataTransfterObject {
    override val id: String get() = this.message_id.toString()
}