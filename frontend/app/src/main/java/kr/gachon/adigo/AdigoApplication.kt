package kr.gachon.adigo

import android.app.Application
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.local.entity.UserLocationEntity
import kr.gachon.adigo.data.local.repository.UserDatabaseRepository
import kr.gachon.adigo.data.local.repository.UserLocationRepository
import kr.gachon.adigo.data.remote.auth.AuthApi
import kr.gachon.adigo.data.remote.auth.AuthRemoteDataSource
import kr.gachon.adigo.data.remote.core.RetrofitProvider
import kr.gachon.adigo.data.remote.friend.FriendApi
import kr.gachon.adigo.data.remote.friend.FriendRemoteDataSource
import kr.gachon.adigo.data.remote.push.PushApi
import kr.gachon.adigo.data.remote.push.PushRemoteDataSource

class AdigoApplication : Application() {
    companion object {
        lateinit var tokenManager: TokenManager
            private set

        lateinit var realm: Realm
            private set

        lateinit var userLocationRepo: UserLocationRepository
            private set

        lateinit var userDatabaseRepo: UserDatabaseRepository
            private set


        // public 노출되는 싱글톤
        lateinit var authRemote: AuthRemoteDataSource
            private set
        lateinit var friendRemote: FriendRemoteDataSource
            private set
        lateinit var pushRemote: PushRemoteDataSource
            private set


    }

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
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


        val retrofit     = RetrofitProvider.create(tokenManager)

        val authApi = retrofit.create(AuthApi::class.java)
        val friendApi = retrofit.create(FriendApi::class.java)
        val pushApi = retrofit.create(PushApi::class.java)

        // 수동 주입
        authRemote = AuthRemoteDataSource(authApi)
        friendRemote = FriendRemoteDataSource(friendApi)
        pushRemote = PushRemoteDataSource(pushApi)

    }
}
