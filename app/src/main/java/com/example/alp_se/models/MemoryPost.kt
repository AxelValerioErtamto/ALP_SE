package com.example.alp_se.models

// The core data structure for a memory post
data class MemoryPost(
    val id: Int,    // The unique ID of the memory
    val userId: Int,      // The ID of the user who created the memory
    val caption: String,
    val imageUrl: String, // URL of the image
    val location: String? // Optional location
)

// Response wrapper for a single MemoryPost object
data class MemoryPostResponse(
    val data: MemoryPost,
    val message: String? = null // Optional success/status message from the API
)

// Response wrapper for a list of MemoryPost objects
data class MemoryPostListResponse(
    val data: List<MemoryPost>,
    val message: String? = null // Optional success/status message from the API
)

// Request model for creating or updating a memory post.
// It doesn't include memoryId (path param for update, generated for create)
// or userId (often inferred from auth token on backend for create).
data class MemoryPostRequest(
    val caption: String,
    val imageUrl: String,
    val location: String?
)