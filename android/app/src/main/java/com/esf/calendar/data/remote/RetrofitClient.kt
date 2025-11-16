package com.esf.calendar.data.remote

import com.esf.calendar.util.Constants
import com.esf.calendar.util.UnsafeOkHttpClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Client Retrofit singleton pour les requêtes API ESF
 */
object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ⚠️ DÉVELOPPEMENT UNIQUEMENT : Utilise un client qui accepte tous les certificats SSL
    private val okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient()
        .newBuilder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.ESF_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ESFApiService = retrofit.create(ESFApiService::class.java)
}
