package kr.gachon.adigo.ui.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.local.transformer.UserTransformer
import kr.gachon.adigo.data.model.dto.CheckDuplicateEmailResponse
import kr.gachon.adigo.data.model.dto.FriendListResponse
import kr.gachon.adigo.data.model.global.User
import retrofit2.Response
import kotlin.collections.orEmpty

/**
 * 400 ms 디바운스로 이메일 중복-검사를 수행하는 ViewModel (DI 프레임워크 X).
 */
class EmailViewModel() : ViewModel() {

    private val remoteDataSource = AdigoApplication.AppContainer.authRemote

    /* ───────────── UI → ViewModel 원본 입력 ───────────── */
    private val _emailInput = MutableStateFlow("")
    val email: StateFlow<String> = _emailInput.asStateFlow()

    /* ─────── 이메일 형식 유효성 / 중복 여부 ─────── */
    private val _emailValid = MutableStateFlow(false)
    val emailValid: StateFlow<Boolean> = _emailValid.asStateFlow()

    private val _emailDuplicate = MutableStateFlow(false)
    val emailDuplicate: StateFlow<Boolean> = _emailDuplicate.asStateFlow()

    init {
        // ❶ 400 ms 디바운스 → ❷ 형식검사 → ❸ 서버 중복-검사
        viewModelScope.launch {
            _emailInput
                .debounce(400)          // 입력 멈춘 뒤 0.4 s
                .distinctUntilChanged() // 같은 값 반복 시 무시
                .collectLatest { current ->
                    /* (A) 형식 검증 */
                    val valid = Patterns.EMAIL_ADDRESS.matcher(current).matches()
                    _emailValid.value = valid

                    if (!valid) {
                        _emailDuplicate.value = false
                        return@collectLatest
                    }

                    val result = remoteDataSource.duplicatedEmail(current)
                    Log.d("emailcheck", result.toString())
                    result.onSuccess {
                        val response: CheckDuplicateEmailResponse? = result.getOrNull()
                        Log.d("emailcheck", response.toString())
                        if(response?.data?.duplicated == false){
                            _emailDuplicate.value = false
                        }else{
                            _emailDuplicate.value = true
                        }
                    }
                        .onFailure {

                        }

                }
        }
    }

    /** Compose TextField → 여기로 전달 */
    fun onEmailChanged(newEmail: String) {
        _emailInput.value = newEmail.trim()
    }
}