package kr.gachon.adigo.data.model.global




/**
 * UI·도메인 계층에서만 쓰는 **순수 데이터 모델**.
 * Realm·Room 같은 DB 라이브러리에 전혀 의존하지 않습니다.
 */
data class UserLocation(
    override val id: String,   // UUID 또는 userId
    val lat: Double,
    val lng: Double
) : BasedManagedModel<UserLocationDto> {
    override fun getDTO(): UserLocationDto {
        return UserLocationDto(id,lat,lng)
    }
}


class UserLocationDto(
    override val id: String,
    val lat: Double,
    val lng: Double

): BasedDataTransfterObject