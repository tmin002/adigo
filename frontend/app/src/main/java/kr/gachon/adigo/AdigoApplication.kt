package kr.gachon.adigo

import android.app.Application
import kr.gachon.adigo.data.local.TokenManager

class AdigoApplication : Application() {
    lateinit var tokenManager: TokenManager

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)

    }
}
