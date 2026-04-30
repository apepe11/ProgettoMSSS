package com.example.progetto.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Se usi l'emulatore Android, 10.0.2.2 punta al "localhost" del tuo computer
    // Se usi un telefono vero, dovrai mettere l'indirizzo IP del tuo PC
    private const val BASE_URL = "http://10.0.2.2:5001/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    val authApiService: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val playlistApiService: PlaylistApi by lazy {
        retrofit.create(PlaylistApi::class.java)
    }


}
