package kr.adigo.adigo.database.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.net.URL

class ChatBubbleEntity: RealmObject {
    lateinit var message: String
    var sender: Int = 0
    var type: Int = 0
}