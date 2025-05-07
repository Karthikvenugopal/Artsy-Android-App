package com.example.artsyandroid.network

import android.content.Context
import com.example.artsyandroid.auth.AuthManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.CookieJar
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitInstance {
    private const val BASE_URL = "https://eng-hangar-456406-u2.wl.r.appspot.com/"

    // Exposed API instance, initialized in MainActivity
    lateinit var api: ArtsyApiService
        private set

    fun init(context: Context) {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val realJar = PersistentCookieJar(
            SetCookieCache(),
            SharedPrefsCookiePersistor(context)
        )
            // 2) Wrap it so Secure‑flag is stripped
        val cookieJar: CookieJar = LenientCookieJar(realJar)

        val client = OkHttpClient.Builder()
            // 1) Network‐interceptor goes before cookieJar()
            .addNetworkInterceptor { chain ->
                val resp = chain.proceed(chain.request())
                // remove any “; Secure” from all Set‑Cookie headers
                val cleaned = resp.headers("Set-Cookie")
                    .map { it.replace(Regex("(?i);\\s*secure"), "") }

                // rebuild headers without the old Set-Cookie, then add back cleaned ones
                val newHeaders = resp.headers.newBuilder()
                    .removeAll("Set-Cookie")
                    .also { hdrs ->
                        cleaned.forEach { hdrs.add("Set-Cookie", it) }
                    }
                    .build()

                resp.newBuilder()
                    .headers(newHeaders)
                    .build()
            }
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = AuthManager.getToken(context)
                val reqBuilder = chain.request().newBuilder()
                if (!token.isNullOrEmpty()) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(reqBuilder.build())
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