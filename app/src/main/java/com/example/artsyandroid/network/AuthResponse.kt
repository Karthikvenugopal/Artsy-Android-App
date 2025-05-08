package com.example.artsyandroid.network

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val fullname: String,
    val email: String,
    val profileImageUrl: String,
    val _id: String
)
