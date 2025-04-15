package kr.gachon.adigo.data.model.global

data class UserLocation(
    var nickname: String,
    val latitude: Double,
    val longitude: Double,
    val profileImageUrl: String? = null

)
