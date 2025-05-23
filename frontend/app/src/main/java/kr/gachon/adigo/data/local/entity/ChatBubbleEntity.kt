package kr.adigo.adigo.database.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.net.URL

class ChatBubbleEntity: RealmObject {
    var message: String = ""
    var sender: String = ""
    var type: String = ""
}