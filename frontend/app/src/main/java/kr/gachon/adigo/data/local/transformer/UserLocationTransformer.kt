package kr.gachon.adigo.data.local.transformer

import kr.gachon.adigo.data.local.entity.UserLocationEntity
import kr.gachon.adigo.data.model.global.UserLocation

/**
 * Realm 엔티티 <--> 순수 모델 간 매퍼.
 *
 * ─ modelToEntity : UI·도메인 계층(UserLocation) → Realm 저장용(UserLocationEntity)
 * ─ entityToModel : Realm 결과(UserLocationEntity)  → UI·도메인 계층(UserLocation)
 */
object UserLocationTransformer : BasedTransformer<UserLocation, UserLocationEntity> {

    /** 모델 → 엔티티 */
    override fun modelToEntity(model: UserLocation): UserLocationEntity =
        UserLocationEntity().apply {
            id  = model.id
            lat = model.lat
            lng = model.lng
        }

    /** 엔티티 → 모델 */
    override fun entityToModel(entity: UserLocationEntity): UserLocation =
        UserLocation(
            id  = entity.id,
            lat = entity.lat,
            lng = entity.lng
        )
}