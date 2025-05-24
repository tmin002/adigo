package kr.gachon.adigo.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.local.repository.UserDatabaseRepository
import kr.gachon.adigo.data.local.transformer.UserTransformer
import kr.gachon.adigo.data.model.dto.FriendListResponse  // 서버 DTO
import kr.gachon.adigo.data.model.dto.ProfileResponse
import kr.gachon.adigo.data.model.global.User     // 도메인 모델
import kr.gachon.adigo.data.remote.friend.FriendApi
import kr.gachon.adigo.data.remote.user.UserApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class MyPageViewModel(
    private val repo: UserDatabaseRepository
) : ViewModel() {
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = AdigoApplication.AppContainer.tokenManager.getCurrentUserId()
            if (userId != null) {
                _currentUser.value = repo.getUserById(userId)
            } else {
                _error.value = "로그인이 필요합니다."
            }
        }
    }

    fun updateProfileImage(imageUri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Uri를 File로 변환
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val file = File(context.cacheDir, "temp_profile_image.jpg")
                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }

                // MultipartBody.Part 생성
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // API 호출
                val response = AdigoApplication.AppContainer.userApi.uploadProfileImage(body)
                
                if (response.isSuccessful) {
                    response.body()?.let { profileResponse ->
                        // 서버 응답에서 프로필 이미지 URL을 가져와서 로컬 DB 업데이트
                        _currentUser.value?.let { user ->
                            val updatedUser = user.apply {
                                profileImageURL = profileResponse.data.profileImage ?: ""
                            }
                            repo.upsert(updatedUser)
                            _currentUser.value = updatedUser
                        }
                    }
                } else {
                    _error.value = "프로필 이미지 업로드 실패: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "프로필 이미지 업로드 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
