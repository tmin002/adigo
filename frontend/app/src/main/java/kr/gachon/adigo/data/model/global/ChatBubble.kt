package kr.gachon.adigo.data.model.global
import java.time.LocalDateTime

data class ChatBubble(
    override val id: Long,
    val message: String,
    val type: ChatBubbleType
): BasedManagedModel<ChatRoomDTO> {
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
    override val id: Long,
    val type: ChatBubbleType,
    val roomId: Long,
    val sender: String,
    val message: String,
    val time: LocalDateTime
) : BasedDataTransfterObject