package kr.gachon.adigo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.gachon.adigo.data.remote.RemoteDataSource
import kr.gachon.adigo.data.local.TokenManager
import kr.gachon.adigo.data.model.LoginResponse


class AuthViewModel(
    private val remoteDataSource: RemoteDataSource,
    private val tokenManager: TokenManager
) : ViewModel() {

    fun login(username: String, password: String) {
        viewModelScope.launch {

        }
    }
}