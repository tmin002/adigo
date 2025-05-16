package kr.gachon.adigo.data.model.global

import java.time.LocalDateTime

data class ChatRoom(
    override val id: Long,
    val targetUserID: Long,
    val createdDate: LocalDateTime
): BasedManagedModel<ChatRoomDTO> {
    override fun getDTO(): ChatRoomDTO {
        TODO("Not yet implemented. 채팅 기능 수정 필요")
    }
}

class ChatRoomDTO(
    override val id: Long,
    val roomName: String,
    val founder_id: Long,
    val created_Date: LocalDateTime
): BasedDataTransfterObject