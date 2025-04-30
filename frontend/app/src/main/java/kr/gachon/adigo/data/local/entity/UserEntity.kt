package kr.adigo.adigo.database.entity

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Realm-side 사용자 테이블
 *
 * * `authority` 는 enum 을 그대로 담을 수 없으므로 문자열로 저장하고
 *   트랜스포머에서 User.Authority 값으로 변환하세요.
 */
class UserEntity : RealmObject {
    @PrimaryKey
    var id: String = ""          // 이메일 = PK

    var name: String = ""
    var profileImageURL: String = ""
    var email: String = ""

    /**  ROLE_ADMIN or ROLE_USER  */
    var authority: String = "ROLE_USER"
}