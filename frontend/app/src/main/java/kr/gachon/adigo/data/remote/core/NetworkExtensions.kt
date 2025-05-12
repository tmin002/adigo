package kr.gachon.adigo.data.remote.core

import retrofit2.HttpException
import retrofit2.Response

// core/NetworkExtensions.kt
suspend inline fun <T> safeCall(
    crossinline block: suspend () -> Response<T>
): Result<T> = try {
    block().run {
        if (isSuccessful && body() != null) Result.success(body()!!)
        else Result.failure(HttpException(this))
    }
} catch (e: Exception) {
    Result.failure(e)
}