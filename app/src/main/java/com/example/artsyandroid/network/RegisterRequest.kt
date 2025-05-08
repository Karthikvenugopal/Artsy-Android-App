package com.example.artsyandroid.network

data class RegisterRequest(
    val fullname: String,
    val email: String,
    val password: String
)
