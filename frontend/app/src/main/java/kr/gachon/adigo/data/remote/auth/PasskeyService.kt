package kr.gachon.adigo.data.remote.auth

import com.google.gson.Gson
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.model.dto.webauthn.AssertionResponseDto


object PasskeyService {
    private val retrofit by lazy { AdigoApplication.AppContainer.retrofit }

    private val api by lazy {
        retrofit.create(WebAuthnApi::class.java)
    }

    suspend fun getAssertionOptions(): Pair<String, String> {
        val resp = api.getAssertionOptions()
        val body = resp.body() ?: throw Exception("Assertion options 불러오기 실패")
        val challengeJson = Gson().toJson(body.publicKeyCredentialRequestOptions)
        return Pair(challengeJson, body.requestId)
    }

    suspend fun verifyAssertion(assertionJson: String) {
        val requestId = /* TODO: 저장된 requestId 로직 필요 */
            throw NotImplementedError("requestId를 저장해서 불러오세요")

        val resp = api.verifyAssertion(
            AssertionResponseDto(requestId, assertionJson)
        )
        if (!resp.isSuccessful) throw Exception("서명 검증 실패: ${resp.code()}")
    }
}