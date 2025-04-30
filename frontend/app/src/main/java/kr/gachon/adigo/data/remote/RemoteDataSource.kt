package kr.gachon.adigo.data.remote

import kr.gachon.adigo.data.model.dto.CheckDuplicateEmailResponse
import kr.gachon.adigo.data.model.dto.CheckDuplicateNumberResponse
import kr.gachon.adigo.data.model.dto.FriendListResponse
import kr.gachon.adigo.data.model.dto.LoginRequest
import kr.gachon.adigo.data.model.dto.LoginResponse
import kr.gachon.adigo.data.model.dto.RefreshTokenRequest
import kr.gachon.adigo.data.model.dto.SignUpRequest
import kr.gachon.adigo.data.model.dto.SignUpResponse
import kr.gachon.adigo.data.model.dto.newPushTokenDto
import kr.gachon.adigo.data.model.dto.newPushTokenResponseDto
import kr.gachon.adigo.data.model.dto.smsResponse
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

    override suspend fun registerDeviceToken(request: newPushTokenDto): Response<newPushTokenResponseDto> =
        apiService.registerDeviceToken(request)

    override suspend fun getFriendList(): Response<FriendListResponse> =
        apiService.getFriendList()

}
