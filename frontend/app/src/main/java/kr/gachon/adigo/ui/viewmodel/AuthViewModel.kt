package kr.gachon.adigo.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.model.dto.LoginRequest
import kr.gachon.adigo.data.model.dto.SignUpRequest
import kr.gachon.adigo.data.model.dto.newPushTokenDto

class AuthViewModel : ViewModel() {

    /* ───────── 전역 싱글턴 참조 ───────── */
    private val authRemote  = AdigoApplication.AppContainer.authRemote
    private val pushRemote  = AdigoApplication.AppContainer.pushRemote
    private val tokenMgr    = AdigoApplication.AppContainer.tokenManager

    private val _isLoggedIn = MutableStateFlow(tokenMgr.hasValidToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn


    /* ───────────── 로그인 ───────────── */
    fun sendLogin(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        authRemote.login(LoginRequest(email, password))
            .onSuccess { result ->
                result?.data?.let {
                    _isLoggedIn.value = true
                    tokenMgr.saveTokens(it)
                    tokenMgr.saveUserEmail(email)
                }

                // FCM 토큰을 서버에 등록 (토큰이 있을 때만)
                registerDeviceTokenIfExists()

                withContext(Dispatchers.Main) { onSuccess() }
            }
            .onFailure { e ->
                withContext(Dispatchers.Main) { onError(e.message ?: "로그인 실패") }
            }
    }

    /* ───────────── 회원가입 ───────────── */
    fun signUp(
        email: String,
        phone: String,
        name: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        authRemote.signup(SignUpRequest(email, password, name, phone))
            .onSuccess {
                // 가입 성공 후 바로 로그인 시도
                sendLogin(email, password, onSuccess, onError)
            }
            .onFailure { e ->
                withContext(Dispatchers.Main) { onError("가입 실패: ${e.message}") }
            }
    }

    /* ───────────── FCM 토큰 등록 ───────────── */
    private suspend fun registerDeviceTokenIfExists() {
        val fcm = tokenMgr.getDeviceToken()
        Log.d("FCM", "token from TokenManager = '$fcm'")   // ★ 토큰 값 확인

        if(fcm.isNullOrBlank()) return

        pushRemote.register(newPushTokenDto(fcm.toString()))
            .onFailure { Log.e("AuthViewModel", "FCM 등록 실패: ${it.message}") }
    }

    /* ──────────── SMS 인증 관련 ──────────── */
    fun sendVerificationCode(
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        authRemote.sendSMS(phoneNumber)
            .onSuccess { withContext(Dispatchers.Main) { onSuccess() } }
            .onFailure { e ->
                withContext(Dispatchers.Main) { onError("인증번호 전송 실패: ${e.message}") }
            }
    }

    fun verifyCode(
        phoneNumber: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        authRemote.verifySMS(phoneNumber, code)
            .onSuccess { res ->
                val ok = res?.data?.success == true
                withContext(Dispatchers.Main) {
                    if (ok) onSuccess() else onError("인증번호 검증 실패")
                }
            }
            .onFailure { e ->
                withContext(Dispatchers.Main) { onError("인증번호 검증 실패: ${e.message}") }
            }
    }

    /* ───────────── 로그아웃 ───────────── */
    fun logout(onComplete: () -> Unit = {}) = viewModelScope.launch {
        tokenMgr.clearTokens()
        _isLoggedIn.value = false
        onComplete()
    }

    fun notifyLoginSuccess() {
        _isLoggedIn.value = true       // ← 로그인 성공 시 호출
    }
}