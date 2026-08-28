package com.dragonfilm.app.data.api

import com.dragonfilm.app.util.DeviceInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "https://dragonfilm.pages.dev"

    val gson: Gson = GsonBuilder()
        .create()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", DeviceInfo.userAgent)
                    .addHeader("X-Device-Model", DeviceInfo.deviceName)
                    .addHeader("X-Device-OS", "Android ${DeviceInfo.osVersion} (API ${DeviceInfo.sdkInt})")
                    .addHeader("X-Device-Brand", DeviceInfo.brand)
                    .addHeader("X-App-Version", DeviceInfo.appVersion)
                    .addHeader("X-Platform", "android")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val service: DragonFilmApiService by lazy {
        retrofit.create(DragonFilmApiService::class.java)
    }
}
