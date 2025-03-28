package kr.adigo.adigo.database.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.net.URL

class UserEntity: RealmObject {
    @PrimaryKey
    var id: String = "";
    var name: String = "";
    var profileImageURL: String = "";
    var email: String = "";
}