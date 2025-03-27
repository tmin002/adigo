package kr.gachon.adigo.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.model.LoginRequest
import kr.gachon.adigo.data.model.LoginResponse
import kr.gachon.adigo.data.remote.ApiService


class AuthViewModel(private val remoteDataSource: ApiService,
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

                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("로그인에 실패하였습니다: ${response.code()}")
                }
            }
        }
    }

}
