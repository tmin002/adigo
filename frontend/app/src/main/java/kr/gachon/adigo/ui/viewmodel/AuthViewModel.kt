package kr.gachon.adigo.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.model.LoginRequest
import kr.gachon.adigo.data.model.LoginResponse
import kr.gachon.adigo.data.model.SignUpRequest
import kr.gachon.adigo.data.model.smsResponse
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

    fun sendVerificationCode(phonenumber: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val response = remoteDataSource.sendSMS(phonenumber)
            if(response.isSuccessful){
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("인증번호 전송에 실패하였습니다: ${response.code()}")
                }
            }

        }
    }

    fun verifyCode(phonenumber: String, code: String, onSuccess: () -> Unit, onError: (String) -> Unit){
        viewModelScope.launch {
            val response = remoteDataSource.verifySMS(phonenumber, code)
            if(response.isSuccessful){
                var body: smsResponse? = response.body()
                body?.let{ smsResponse ->
                    if(smsResponse.data.success == true){
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    }else{
                        withContext(Dispatchers.Main) {
                            onError("인증번호 검증가 틀렸습니다543: ${response.code()}")
                        }
                    }
                }

            } else {
                withContext(Dispatchers.Main) {
                    onError("인증번호 검증을 서버에 요청하는것에 실패하였습니다: ${response.code()}")
                }
                }
            }

        }

    fun signUp(email: String, phone: String, name: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit){
        viewModelScope.launch {
            val response = remoteDataSource.signup(SignUpRequest(email, password, name, phone))
            if(response.isSuccessful){

                withContext(Dispatchers.Main) {
                    //회원가입 이후 로그인 함수 호출해서 jwt토큰 저장하기.
                    sendLogin(email, password, onSuccess, onError)
                    onSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("가입에 실패하였습니다: ${response.code()}")
                }
            }


        }
    }
    }

