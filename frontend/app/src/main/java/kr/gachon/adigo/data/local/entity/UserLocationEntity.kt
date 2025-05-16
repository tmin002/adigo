package kr.gachon.adigo.data.local.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class UserLocationEntity : RealmObject {
    @PrimaryKey
    var id: Long = 0L
    var lat: Double = 0.0
    var lng: Double = 0.0
}