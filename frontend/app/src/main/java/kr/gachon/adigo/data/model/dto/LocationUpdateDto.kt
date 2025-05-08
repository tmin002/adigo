package kr.gachon.adigo.data.model.dto

// Matches the JSON structure for /app/location/update
data class LocationUpdateDto(
    val latitude: Double,
    val longitude: Double
) 