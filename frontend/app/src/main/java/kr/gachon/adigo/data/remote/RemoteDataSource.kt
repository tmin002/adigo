package kr.gachon.adigo.data.remote

import kr.gachon.adigo.data.model.LoginRequest
import kr.gachon.adigo.data.model.LoginResponse
import retrofit2.Response


// 3. RemoteDataSource.kt
abstract class RemoteDataSource(private val apiService: ApiService) : ApiService {

    suspend fun login(username: String, password: String): Response<LoginResponse> =
        apiService.login(LoginRequest(username, password))

}