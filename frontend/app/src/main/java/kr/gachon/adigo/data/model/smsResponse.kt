package kr.gachon.adigo.data.model

data class smsResponse(
    val status: Int,
    val message: String,
    val data: responsedata
) {
    data class responsedata(
        val success: Boolean
    )
}
