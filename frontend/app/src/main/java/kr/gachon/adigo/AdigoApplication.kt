package kr.gachon.adigo

import android.app.Application
import com.google.gson.Gson
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
import kr.gachon.adigo.data.remote.websocket.StompWebSocketClient
import kr.gachon.adigo.data.remote.websocket.UserLocationWebSocketReceiver
import kr.gachon.adigo.data.remote.websocket.UserLocationWebSocketSender

class AdigoApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    companion object {
        lateinit var tokenManager: TokenManager
            private set

        lateinit var realm: Realm
            private set

        lateinit var userLocationRepo: UserLocationRepository
            private set

        lateinit var userDatabaseRepo: UserDatabaseRepository
            private set

        lateinit var gson: Gson
            private set

        lateinit var stompWebSocketClient: StompWebSocketClient
            private set
        lateinit var userLocationWebSocketReceiver: UserLocationWebSocketReceiver
            private set
        lateinit var userLocationWebSocketSender: UserLocationWebSocketSender
            private set
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
        gson = Gson()

        val config = RealmConfiguration
            .Builder(schema = setOf(
                UserLocationEntity::class,
                UserEntity::class))
            .deleteRealmIfMigrationNeeded()
            .build()
        realm = Realm.open(config)

        userLocationRepo = UserLocationRepository(realm)
        userDatabaseRepo = UserDatabaseRepository(realm)

        val okHttpClient = RetrofitProvider.getOkHttpClient(tokenManager)
        val retrofit     = RetrofitProvider.create(tokenManager)

        val authApi = retrofit.create(AuthApi::class.java)
        val friendApi = retrofit.create(FriendApi::class.java)
        val pushApi = retrofit.create(PushApi::class.java)

        authRemote = AuthRemoteDataSource(authApi)
        friendRemote = FriendRemoteDataSource(friendApi)
        pushRemote = PushRemoteDataSource(pushApi)

        stompWebSocketClient = StompWebSocketClient(
            websocketUrl = "wss://adigo.site/api/ws/chat/websocket",
            tokenManager = tokenManager,
            okHttpClient = okHttpClient,
            applicationScope = applicationScope
        )

        userLocationWebSocketReceiver = UserLocationWebSocketReceiver(
            stompClient = stompWebSocketClient,
            userLocationRepository = userLocationRepo,
            gson = gson,
            coroutineScope = applicationScope
        )

        userLocationWebSocketSender = UserLocationWebSocketSender(
            stompClient = stompWebSocketClient,
            gson = gson
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        realm.close()
        stompWebSocketClient.shutdown()
    }
}
