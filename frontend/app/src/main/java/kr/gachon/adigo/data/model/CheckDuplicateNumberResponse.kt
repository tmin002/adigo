package kr.gachon.adigo.data.model

data class CheckDuplicateNumberResponse (
    val status: Int,
    val message: String,
    val data: ResponseData
) {
    data class ResponseData(
        val isDuplicated: Boolean
    )
}