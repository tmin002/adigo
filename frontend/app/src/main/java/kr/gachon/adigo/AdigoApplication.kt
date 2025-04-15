package kr.gachon.adigo

import android.app.Application
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.remote.ApiService
import kr.gachon.adigo.data.remote.httpClient

class AdigoApplication : Application() {
    companion object {
        lateinit var tokenManager: TokenManager
            private set

        lateinit var httpService: ApiService
            private set

    }

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        httpService = httpClient.create(tokenManager)
    }
}
