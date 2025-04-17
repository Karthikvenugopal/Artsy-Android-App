package com.example.artsyandroid.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // For an Android emulator, 10.0.2.2 maps to the host's localhost.
    private const val BASE_URL = "http://10.0.2.2:5001/"

    val api: ArtsyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ArtsyApiService::class.java)
    }
}
