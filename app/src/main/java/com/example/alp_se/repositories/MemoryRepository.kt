package com.example.alp_se.repositories


import com.example.alp_se.models.MemoryPost
import com.example.alp_se.models.MemoryPostRequest
import com.example.alp_se.services.MemoryAPIService
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume

/**
 * Helper extension function to convert a Retrofit Call into a Kotlin Result
 * within a suspend function. This handles the asynchronous nature of network calls
 * and wraps responses/errors appropriately.
 *
 * @param T The type of the successful response body from Retrofit.
 * @param R The type of the data to be emitted in Result.success.
 * @param transform A lambda function to convert the Retrofit response body (T)
 *                  into the desired data type (R).
 * @return Result<R> Either Result.success with the transformed data or Result.failure with an exception.
 */
private suspend fun <T, R> Call<T>.awaitResult(transform: (T) -> R): Result<R> {
    // 'this' refers to the Call<T> instance on which awaitResult is invoked.
    // We can use a labeled 'this' or assign it to a variable to be captured.
    val originalCall = this // Assigning to a variable for clarity in the lambda

    return suspendCancellableCoroutine { continuation ->
        originalCall.enqueue(object : Callback<T> { // Use originalCall here
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (continuation.isCancelled) return

                try {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            continuation.resume(Result.success(transform(body)))
                        } else {
                            continuation.resume(Result.failure(Exception("Response body is null")))
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "API call failed with code ${response.code()}"
                        continuation.resume(Result.failure(Exception(errorMsg)))
                    }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                if (continuation.isCancelled) return
                continuation.resume(Result.failure(t))
            }
        })

        continuation.invokeOnCancellation {
            try {
                // Use 'originalCall' which refers to the Call<T> instance
                // on which awaitResult was invoked.
                if (originalCall.isExecuted && !originalCall.isCanceled) {
                    originalCall.cancel()
                }
            } catch (ex: Exception) {
                // Log or handle cancellation exception if necessary, but don't crash
            }
        }
    }
}

class MemoryRepository(private val memoryAPIService: MemoryAPIService) {

    suspend fun fetchMyMemories(token: String): Result<List<MemoryPost>> {
        return memoryAPIService.getMyMemories(token)
            .awaitResult { memoryPostList -> // Response is directly List<MemoryPost>
                memoryPostList
            }
    }

    suspend fun fetchAllMemories(token: String): Result<List<MemoryPost>> {
        return memoryAPIService.getAllMemories(token)
            .awaitResult { memoryPostList -> // Response is directly List<MemoryPost>
                memoryPostList
            }
    }

    suspend fun createMemory(token: String, caption: String, imageUriString: String, location: String?): Result<MemoryPost> {
        val request = MemoryPostRequest(
            caption = caption,
            imageUrl = imageUriString,
            location = location
        )
        return memoryAPIService.createMemory(token, request)
            .awaitResult { memoryPostResponse ->
                memoryPostResponse.data
            }
    }

    suspend fun updateMemory(token: String, memoryId: Int, caption: String, imageUriString: String, location: String?): Result<MemoryPost> {
        val request = MemoryPostRequest(
            caption = caption,
            imageUrl = imageUriString,
            location = location
        )
        return memoryAPIService.updateMemory(token, memoryId, request)
            .awaitResult { memoryPostResponse ->
                memoryPostResponse.data
            }
    }

    suspend fun deleteMemory(token: String, memoryId: Int): Result<Boolean> {
        return memoryAPIService.deleteMemory(token, memoryId)
            .awaitResult {
                true
            }
    }

    suspend fun getMemoryById(token: String, memoryId: Int): Result<MemoryPost> {
        return memoryAPIService.getMemoryById(token, memoryId)
            .awaitResult { memoryPostResponse ->
                memoryPostResponse.data
            }
    }
}