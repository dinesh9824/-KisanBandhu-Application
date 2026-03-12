package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://kisanbandhu-backend.onrender.com/"
    private const val GOV_BASE_URL = "https://api.data.gov.in/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val marketApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GOV_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}