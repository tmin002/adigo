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
import retrofit2.Retrofit

class AdigoApplication : Application() {

    /** 모든 싱글턴을 담아 둘 전역 컨테이너 */
    object AppContainer {
        // ─── 런타임에 채워짐 ───
        lateinit var tokenManager: TokenManager
        lateinit var realm: Realm
        lateinit var gson: Gson

        lateinit var userLocationRepo: UserLocationRepository
        lateinit var userDatabaseRepo: UserDatabaseRepository

        lateinit var retrofit: Retrofit
        lateinit var authRemote: AuthRemoteDataSource
        lateinit var friendRemote: FriendRemoteDataSource
        lateinit var pushRemote: PushRemoteDataSource

        lateinit var stompClient: StompWebSocketClient
        lateinit var wsReceiver: UserLocationWebSocketReceiver
        lateinit var wsSender: UserLocationWebSocketSender
    }

    override fun onCreate() {
        super.onCreate()

        val container = AppContainer               // 짧게 alias
        container.tokenManager = TokenManager(this)
        container.gson         = Gson()

        // ─ Realm ─
        container.realm = Realm.open(
            RealmConfiguration.Builder(
                setOf(UserLocationEntity::class, UserEntity::class)
            ).deleteRealmIfMigrationNeeded()
                .build()
        )
        container.userLocationRepo = UserLocationRepository(container.realm)
        container.userDatabaseRepo = UserDatabaseRepository(container.realm)

        // ─ Retrofit & Remote DS ─
        val okHttp = RetrofitProvider.getOkHttpClient(container.tokenManager)
        container.retrofit = RetrofitProvider.create(container.tokenManager)

        container.authRemote   = AuthRemoteDataSource(container.retrofit.create(AuthApi::class.java))
        container.friendRemote = FriendRemoteDataSource(container.retrofit.create(FriendApi::class.java))
        container.pushRemote   = PushRemoteDataSource(container.retrofit.create(PushApi::class.java))

        // ─ WebSocket ─
        val appScope = CoroutineScope(SupervisorJob())
        container.stompClient = StompWebSocketClient(
            websocketUrl   = "wss://adigo.site/api/ws/chat/websocket",
            tokenManager   = container.tokenManager,
            okHttpClient   = okHttp,
            applicationScope = appScope
        )
        container.wsReceiver = UserLocationWebSocketReceiver(
            stompClient = container.stompClient,
            userLocationRepository = container.userLocationRepo,
            gson = container.gson,
            coroutineScope = appScope
        )
        container.wsSender = UserLocationWebSocketSender(
            stompClient = container.stompClient,
            gson = container.gson
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        AppContainer.realm.close()
        AppContainer.stompClient.shutdown()
    }
}