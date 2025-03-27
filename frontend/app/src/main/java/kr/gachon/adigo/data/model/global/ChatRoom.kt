package kr.gachon.adigo.data.model.global

import java.time.LocalDateTime

data class ChatRoom(
    override val id: String,
    val targetUserID: String,
    val createdDate: LocalDateTime
): IManagedModel<ChatRoomDTO> {
    override fun getDTO(): ChatRoomDTO {
        TODO("Not yet implemented. 채팅 기능 수정 필요")
    }
}

class ChatRoomDTO(
    val roomId: String,
    val roomName: String,
    val founder_id: Long,
    val created_Date: LocalDateTime
): IDataTransfterObject {
    override val id: String get() = this.roomId
}