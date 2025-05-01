package kr.gachon.adigo

import android.app.Application
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.local.entity.UserLocationEntity
import kr.gachon.adigo.data.local.repository.UserDatabaseRepository
import kr.gachon.adigo.data.local.repository.UserLocationRepository
import kr.gachon.adigo.data.remote.ApiService
import kr.gachon.adigo.data.remote.httpClient

class AdigoApplication : Application() {
    companion object {
        lateinit var tokenManager: TokenManager
            private set

        lateinit var httpService: ApiService

        lateinit var realm: Realm
            private set

        lateinit var userLocationRepo: UserLocationRepository
            private set

        lateinit var userDatabaseRepo: UserDatabaseRepository
            private set


    }

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        httpService = httpClient.create(tokenManager)
        val config = RealmConfiguration
            .Builder(schema = setOf(
                UserLocationEntity::class,
                UserEntity::class))
            .deleteRealmIfMigrationNeeded()
            .build()
        realm = Realm.open(config)
        // ② Repository 생성
        userLocationRepo = UserLocationRepository(realm)
        userDatabaseRepo = UserDatabaseRepository(realm)
    }
}
