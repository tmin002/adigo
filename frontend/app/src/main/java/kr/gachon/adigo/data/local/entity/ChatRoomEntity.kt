package kr.adigo.adigo.database.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class ChatRoomEntity : RealmObject {
    @PrimaryKey
    var id: String = "";
    var targetUserID: String = "";
}
