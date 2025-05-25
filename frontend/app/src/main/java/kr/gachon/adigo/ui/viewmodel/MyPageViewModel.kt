package kr.gachon.adigo.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.local.repository.UserDatabaseRepository
import kr.gachon.adigo.data.local.transformer.UserTransformer
import kr.gachon.adigo.data.model.dto.FriendListResponse  // 서버 DTO
import kr.gachon.adigo.data.model.dto.ProfileResponse
import kr.gachon.adigo.data.model.global.User     // 도메인 모델
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MyPageViewModel(
    private val userDatabaseRepo: UserDatabaseRepository
) : ViewModel() {
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val response = AdigoApplication.AppContainer.userRemote.getCurrentUser()
                if (response.isSuccessful) {
                    response.body()?.data?.let { user ->
                        val userEntity = UserTransformer.modelToEntity(user)
                        userDatabaseRepo.upsert(userEntity)
                        _currentUser.value = userEntity
                    }
                } else {
                    _error.value = "사용자 정보를 불러오는데 실패했습니다"
                }
            } catch (e: Exception) {
                _error.value = "사용자 정보를 불러오는데 실패했습니다: ${e.message}"
            }
        }
    }

    fun updateProfileImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = createMultipartBodyPart(uri, context)
                val response = AdigoApplication.AppContainer.userRemote.uploadProfileImage(file)
                if (response.isSuccessful) {
                    response.body()?.data?.let { user ->
                        val currentUser = _currentUser.value
                        val updatedUser = user.copy(
                            email = currentUser?.email ?: user.email,
                            nickname = currentUser?.name ?: user.nickname,
                            authority = User.Authority.valueOf(currentUser?.authority ?: user.authority.name)
                        )
                        val userEntity = UserTransformer.modelToEntity(updatedUser)
                        userDatabaseRepo.upsert(userEntity)
                        _currentUser.value = userEntity
                    }
                    loadCurrentUser()
                } else {
                    _error.value = "프로필 이미지 업로드에 실패했습니다"
                }
            } catch (e: Exception) {
                _error.value = "프로필 이미지 업로드에 실패했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = AdigoApplication.AppContainer.userRemote.updateNickname(nickname)
                if (response.isSuccessful) {
                    response.body()?.data?.let { user ->
                        val currentUser = _currentUser.value
                        val updatedUser = user.copy(
                            email = currentUser?.email ?: user.email,
                            nickname = nickname,
                            authority = User.Authority.valueOf(currentUser?.authority ?: user.authority.name)
                        )
                        val userEntity = UserTransformer.modelToEntity(updatedUser)
                        userDatabaseRepo.upsert(userEntity)
                        _currentUser.value = userEntity
                    }
                    loadCurrentUser()
                } else {
                    _error.value = "닉네임 변경에 실패했습니다"
                }
            } catch (e: Exception) {
                _error.value = "닉네임 변경에 실패했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun createMultipartBodyPart(uri: Uri, context: Context): MultipartBody.Part {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File.createTempFile("profile_image", ".jpg", context.cacheDir)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", file.name, requestFile)
    }
}
