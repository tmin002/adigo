package kr.adigo.adigo.database.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.net.URL

class ChatRoomEntity : RealmObject {
    @PrimaryKey
    lateinit var id: String;
    lateinit var targetUserID: String;
}
