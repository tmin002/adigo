package kr.gachon.adigo.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.gachon.adigo.data.model.CheckDuplicateEmailResponse
import kr.gachon.adigo.data.model.LoginRequest
import kr.gachon.adigo.data.model.LoginResponse
import kr.gachon.adigo.data.remote.ApiService

class EmailViewModel(private val remoteDataSource: ApiService) : ViewModel() {
    val email = mutableStateOf("")
    val emailValid = mutableStateOf(false)
    val emailDuplicate = mutableStateOf(false)


    // 이메일 형식 정규식 검증
    private fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun onEmailChanged(newEmail: String) {
        email.value = newEmail
        emailValid.value = isEmailValid(newEmail)

        if (emailValid.value) {
            checkEmailDuplicate(newEmail)
        } else {
            emailDuplicate.value = false
        }
    }

    // 가상의 서버 중복 체크 (실제로는 서버 API 호출)
    private fun checkEmailDuplicate(email: String){
        viewModelScope.launch {

            val response = remoteDataSource.checkDuplicateEmail(email)
            if (response.isSuccessful) {
                val body: CheckDuplicateEmailResponse? = response.body()
                body?.let { Response ->
                    emailDuplicate.value = Response.data.duplicated
                }
            } else {
                // 에러 처리
            }
        }
    }
}
