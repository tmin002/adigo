package kr.gachon.adigo.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.model.LoginRequest
import kr.gachon.adigo.data.model.LoginResponse
import kr.gachon.adigo.data.remote.ApiService
import kr.gachon.adigo.data.remote.RemoteDataSource


class ContentViewModel(private val remoteDataSource: ApiService,
                       private val tokenManager: TokenManager) : ViewModel() {



    var isLoading: Boolean = false
        private set

    // 로그인 API
    fun sendLogin(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val response = remoteDataSource.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body: LoginResponse? = response.body()
                body?.let { loginResponse ->
                    tokenManager.saveTokens(loginResponse.data)
                }
            } else {
                // 에러 처리
            }
        }
    }

}
