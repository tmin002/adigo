package kr.gachon.adigo.data.model

data class CheckDuplicateEmailResponse(
    val status: Int,
    val message: String,
    val data: ResponseData
) {
    data class ResponseData(
        val duplicated: Boolean
    )
}
