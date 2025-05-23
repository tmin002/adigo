package kr.gachon.adigo.data.remote.core

import retrofit2.HttpException
import retrofit2.Response

// core/NetworkExtensions.kt
suspend inline fun <T> safeCall(
    crossinline block: suspend () -> Response<T>
): Result<T> = try {
    val response = block()
    try {
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(HttpException(response))
        }
    } finally {
        response.errorBody()?.close()
    }
} catch (e: Exception) {
    Result.failure(e)
}