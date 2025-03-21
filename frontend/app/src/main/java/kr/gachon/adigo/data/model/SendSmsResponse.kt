package kr.gachon.adigo.data.model

data class SendSmsResponse(
    val status: Int,
    val message: String,
    val data: SmsResponseData
) {
    data class SmsResponseData(
        val success: Boolean
    )
}
