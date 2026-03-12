package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("/predict")
    fun getCrop(@Body request: CropRequest): Call<CropResponse>

    @Multipart
    @POST("/predict_disease")
    fun predictDisease(
        @Part image: MultipartBody.Part
    ): Call<DiseaseResponse>

    @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
    fun getMarketPrices(
        @Query("api-key") apiKey: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 10,
        @Query("filters[state]") state: String? = null,
        @Query("filters[commodity]") commodity: String? = null
    ): Call<MarketResponse>
}