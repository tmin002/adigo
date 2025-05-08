package kr.gachon.adigo.data.model.dto

// Matches the structure for each friend location in the response list
data class FriendLocationDto(
    val id: String,           // userId or unique identifier
    val latitude: Double,
    val longitude: Double
) 