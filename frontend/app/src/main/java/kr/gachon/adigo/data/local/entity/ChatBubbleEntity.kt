package kr.adigo.adigo.database.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.net.URL

class ChatBubbleEntity: RealmObject {
    lateinit var message: String
    lateinit var sender: String
    lateinit var type: String
}