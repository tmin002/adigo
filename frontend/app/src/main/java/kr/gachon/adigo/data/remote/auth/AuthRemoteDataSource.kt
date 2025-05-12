package kr.gachon.adigo.data.remote.auth

import kr.gachon.adigo.data.model.dto.LoginRequest
import kr.gachon.adigo.data.model.dto.RefreshTokenRequest
import kr.gachon.adigo.data.model.dto.SignUpRequest
import kr.gachon.adigo.data.remote.core.safeCall

// auth/AuthRemoteDataSource.kt
class AuthRemoteDataSource (
    private val api: AuthApi
) {
    suspend fun login(req: LoginRequest)        = safeCall { api.login(req) }
    suspend fun signup(req: SignUpRequest)      = safeCall { api.signup(req) }
    suspend fun sendSMS(to: String)             = safeCall { api.sendSMS(to) }
    suspend fun verifySMS(to: String, code: String) =
        safeCall { api.verifySMS(to, code) }
    suspend fun duplicatedEmail(e: String)      = safeCall { api.duplicatedEmail(e) }
    suspend fun duplicatedNumber(p: String)     = safeCall { api.duplicatedNumber(p) }
    suspend fun refresh(req: RefreshTokenRequest)= safeCall { api.refresh(req) }
}