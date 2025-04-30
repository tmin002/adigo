package kr.gachon.adigo.data.model;



data class newPushTokenResponseDto(
        val status: Int,
        val message: String,
        val data: Response
) {
    data class Response(
            var success: Boolean
    )
}





