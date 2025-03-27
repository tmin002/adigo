package kr.gachon.adigo.data.remote

import kr.gachon.adigo.data.model.*
import retrofit2.Response

class RemoteDataSource(private val apiService: ApiService) : ApiService {

    override suspend fun login(request: LoginRequest): Response<LoginResponse> =
        apiService.login(request)

    override suspend fun signup(request: SignUpRequest): Response<SignUpResponse> =
        apiService.signup(request)

    override suspend fun sendSMS(phoneNumber: String): Response<smsResponse> =
        apiService.sendSMS(phoneNumber)

    override suspend fun verifySMS(phoneNumber: String, code: String): Response<smsResponse> =
        apiService.verifySMS(phoneNumber, code)

    override suspend fun checkDuplicateEmail(email: String): Response<CheckDuplicateEmailResponse> =
        apiService.checkDuplicateEmail(email)

    override suspend fun checkDuplicateNumber(number: String): Response<CheckDuplicateNumberResponse> =
        apiService.checkDuplicateNumber(number)

    override suspend fun refreshToken(request: RefreshTokenRequest): Response<LoginResponse> =
        apiService.refreshToken(request)
}