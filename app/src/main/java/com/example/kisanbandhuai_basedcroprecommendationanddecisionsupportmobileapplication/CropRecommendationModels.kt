package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

data class CropRequest(
    val N: Float,
    val P: Float,
    val K: Float,
    val temperature: Float,
    val humidity: Float,
    val ph: Float,
    val rainfall: Float
)

data class CropPrediction(
    val cropName: String,
    val probability: Float
)

data class CropResponse(
    val success: Boolean,
    val recommended_crop: String,
    val top_predictions: List<CropPrediction> = emptyList()
)