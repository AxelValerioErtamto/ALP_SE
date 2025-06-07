package com.example.alp_se.models

data class UserResponse(
    val data: User
)

data class User (
    val id: Int,
    val username: String,
    val token: String?
)