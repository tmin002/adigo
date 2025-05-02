package kr.gachon.adigo.ui.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.AdigoApplication.Companion.tokenManager
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.model.dto.CheckDuplicateEmailResponse
import kr.gachon.adigo.data.model.dto.LoginRequest
import kr.gachon.adigo.data.model.dto.LoginResponse
import kr.gachon.adigo.data.model.dto.SignUpRequest
import kr.gachon.adigo.data.model.dto.SignUpResponse
import kr.gachon.adigo.data.model.dto.newPushTokenDto
import kr.gachon.adigo.data.model.dto.newPushTokenResponseDto
import kr.gachon.adigo.data.model.dto.smsResponse



class AuthViewModel() : ViewModel() {


    var isLoading: Boolean = false
        private set

    var remoteDataSource = AdigoApplication.authRemote
    var pushDataSource = AdigoApplication.pushRemote

    // 로그인 API
    fun sendLogin(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {

        viewModelScope.launch {
            val response = remoteDataSource.login(LoginRequest(email, password))
            response.onSuccess {
                val response: LoginResponse? = response.getOrNull()
                response?.let { loginResponse ->
                    tokenManager.saveTokens(loginResponse.data)
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }

                }
            }
                .onFailure {

                }


        }
    }

    fun sendDeviceToken(){
        viewModelScope.launch {

            val token: String? = tokenManager.getDeviceToken()

            val response = pushDataSource.register(newPushTokenDto(token.toString()))
            response.onSuccess {
                val response: newPushTokenResponseDto? = response.getOrNull()
                Log.d("push","token 성공적으로 보냄: " + response)
                }
                .onFailure {
                    withContext(Dispatchers.Main) {
                        Log.d("push","token 보내기 실패")
                    }
                }

        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            tokenManager.clearTokens()
            onComplete()
        }
    }

    fun sendVerificationCode(phonenumber: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val response = remoteDataSource.sendSMS(phonenumber)

            response.onSuccess {
                val response: smsResponse? = response.getOrNull()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
                .onFailure {
                    withContext(Dispatchers.Main) {
                        onError("인증번호 전송에 실패하였습니다")
                    }
                }

        }
    }

    fun verifyCode(phonenumber: String, code: String, onSuccess: () -> Unit, onError: (String) -> Unit){
        viewModelScope.launch {
            val response = remoteDataSource.verifySMS(phonenumber, code)
            response.onSuccess {
                val response: smsResponse? = response.getOrNull()
                if(response?.data?.success == true) {
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }else{
                    withContext(Dispatchers.Main) {
                        onError("인증번호 검증에 실패하였습니다: ${response?.data?.success}")
                    }
                }

            }.onFailure {
                withContext(Dispatchers.Main) {
                    onError("인증번호 검증에 실패하였습니다")
                }
            }
        }

        }

    fun signUp(email: String, phone: String, name: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit){
        viewModelScope.launch {
            val response = remoteDataSource.signup(SignUpRequest(email, password, name, phone))
            response.onSuccess {
                val response: SignUpResponse? = response.getOrNull()
                withContext(Dispatchers.Main) {
                    sendLogin(email, password, onSuccess, onError)
                }
            } .onFailure {
                withContext(Dispatchers.Main) {
                    onError("가입에 실패하였습니다")
                }
            }




        }
    }
    }

