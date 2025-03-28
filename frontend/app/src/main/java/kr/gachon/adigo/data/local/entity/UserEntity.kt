package kr.adigo.adigo.database.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.net.URL

class UserEntity: RealmObject {
    @PrimaryKey
    lateinit var id: String;
    lateinit var name: String;
    lateinit var profileImageURL: URL;
    lateinit var email: String;
}