package kr.gachon.adigo.data.remote.push

import kr.gachon.adigo.data.model.dto.newPushTokenDto
import kr.gachon.adigo.data.remote.core.safeCall

class PushRemoteDataSource (
    private val api: PushApi
) {
    suspend fun register(dto: newPushTokenDto) = safeCall { api.register(dto) }
}