package kr.gachon.adigo

import android.app.Application
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.HiltAndroidApp
import kr.gachon.adigo.data.local.TokenManager

@HiltAndroidApp
class AdigoApplication : Application() {
    lateinit var tokenManager: TokenManager

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)

    }
}
