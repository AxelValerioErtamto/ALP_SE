package com.example.alp_se.services

import com.example.alp_se.models.GeneralResponseModel
import com.example.alp_se.models.MemoryPost // Assuming MemoryPost is the direct item in the list
import com.example.alp_se.models.MemoryPostRequest
import com.example.alp_se.models.MemoryPostResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
// No @Query needed for getMyMemories or getAllMemories based on your backend routes

interface MemoryAPIService {

    // Fetches ALL memories (maps to GET /api/memories)
    @GET("api/memories")
    fun getAllMemories(@Header("X-API-TOKEN") token: String): Call<List<MemoryPost>>

    // Fetches memories of the CURRENTLY AUTHENTICATED user (maps to GET /api/memories/user)
    @GET("api/memories/user")
    fun getMyMemories(@Header("X-API-TOKEN") token: String): Call<List<MemoryPost>>

    // Creates a new memory post.
    @POST("api/memories")
    fun createMemory(
        @Header("X-API-TOKEN") token: String,
        @Body memoryRequest: MemoryPostRequest
    ): Call<MemoryPostResponse> // Assuming backend wraps this response

    // Fetches a single memory post by its ID.
    @GET("api/memories/{memoryId}")
    fun getMemoryById(
        @Header("X-API-TOKEN") token: String,
        @Path("memoryId") memoryId: Int
    ): Call<MemoryPostResponse> // Assuming backend wraps this response

    // Updates an existing memory post.
    @PUT("api/memories/{memoryId}")
    fun updateMemory(
        @Header("X-API-TOKEN") token: String,
        @Path("memoryId") memoryId: Int,
        @Body memoryRequest: MemoryPostRequest
    ): Call<MemoryPostResponse> // Assuming backend wraps this response

    // Deletes a memory post.
    @DELETE("api/memories/{memoryId}")
    fun deleteMemory(
        @Header("X-API-TOKEN") token: String,
        @Path("memoryId") memoryId: Int
    ): Call<GeneralResponseModel> // Backend sends { message: "..." }
}