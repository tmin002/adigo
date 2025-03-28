package kr.gachon.adigo.data.local.repository

import kr.adigo.adigo.database.entity.ChatRoomEntity

object ChatRoomDatabaseRepository :
    BasedManagedModelDatabaseRepository<ChatRoomEntity>(ChatRoomEntity::class) {}