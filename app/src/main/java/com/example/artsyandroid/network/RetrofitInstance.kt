package com.example.artsyandroid.network

import android.content.Context
import com.example.artsyandroid.auth.AuthManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "http://10.0.2.2:5001/"

    // Exposed API instance, initialized in MainActivity
    lateinit var api: ArtsyApiService
        private set

    fun init(context: Context) {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = AuthManager.getToken(context)
                val request = chain.request().newBuilder().apply {
                    if (!token.isNullOrEmpty()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }.build()
                chain.proceed(request)
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ArtsyApiService::class.java)
    }
}